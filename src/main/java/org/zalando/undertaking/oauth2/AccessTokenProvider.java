package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;

import org.reactivestreams.Publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zalando.undertaking.oauth2.credentials.RequestCredentials;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import io.reactivex.disposables.Disposable;

import io.reactivex.schedulers.Schedulers;

import io.reactivex.subjects.BehaviorSubject;

/**
 * Retrieves the current access token.
 *
 * <p>As each access token has a lifetime, it is periodically refreshed.</p>
 */
class AccessTokenProvider implements Provider<Single<AccessToken>> {

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenProvider.class);

    private final Single<RequestCredentials> credentials;

    private final AccessTokenRequestProvider requestProvider;

    /**
     * Facade to access OAuth2 configuration values.
     */
    private final AccessTokenSettings settings;

    /**
     * Changes to the access token are published through this interface.
     */
    private final BehaviorSubject<AccessToken> changes = BehaviorSubject.create();
    private final Clock clock;
    private Observable<?> autoUpdater;

    @Inject
    AccessTokenProvider(final Single<RequestCredentials> credentials, final AccessTokenRequestProvider requestProvider,
            final AccessTokenSettings settings, final Clock clock) {
        this.credentials = requireNonNull(credentials);
        this.requestProvider = requireNonNull(requestProvider);
        this.settings = requireNonNull(settings);
        this.clock = requireNonNull(clock);
    }

    Disposable autoUpdate() {
        this.autoUpdater = Single.defer(() -> repeat(update())).toObservable().share();

        return this.autoUpdater.subscribe();
    }

    @Override
    public Single<AccessToken> get() {
        return Single.defer(() -> {
                final AccessToken value = changes.getValue();
                if (value != null) {
                    return Single.just(value);
                }

                return
                    changes.take(1)                                  //
                    .singleOrError().observeOn(Schedulers.computation()) // Used to move out of AsyncHttpClient thread
                    .timeout(1, TimeUnit.SECONDS);
            });
    }

    @VisibleForTesting
    Single<AccessTokenResponse> update() {
        return credentials.flatMap(requestProvider::requestAccessToken)              //
                          .doOnSubscribe((s) -> LOG.info("Requesting access token")) //
                          .doOnSuccess(response -> {
                              final AccessToken accessToken = response.getAccessToken();
                              LOG.info("Updating access token: {}", accessToken);
                              changes.onNext(accessToken);
                          })                                                         //
                          .doOnError(e -> LOG.error("Unable to request access token: [{}]", e.getMessage(), e))
                          .retryWhen(this::scheduleRetries);
    }

    private Publisher<Object> scheduleRetries(final Flowable<?> retryer) {
        final Flowable<Integer> retryDelays = Flowable.fromIterable( //
                FluentIterable.from(Ints.asList(0, 1, 1, 5, 15, 30)).append(Iterables.cycle(60)));

        return retryer.zipWith(retryDelays, (error, delay) -> delay) //
                      .flatMap(delay -> {
                          if (delay > 0) {
                              LOG.info("Retrying access token request in [{}] seconds", delay);
                              return Flowable.timer(delay, SECONDS);
                          } else {
                              LOG.info("Retrying access token request");
                              return Flowable.just(0L);
                          }
                      });
    }

    private Single<AccessTokenResponse> repeat(final Single<AccessTokenResponse> single) {
        // workaround for the lack of Single.repeatWhen()
        // https://github.com/ReactiveX/RxJava/issues/4155

        return single.flatMap(response -> {
                final Instant expiryTime = response.getExpiryTime();
                final Duration delay = calculateRefreshDelay(expiryTime);

                LOG.info("[{}] expires at [{}], scheduling refresh in [{}]", //
                    response.getAccessToken(), expiryTime, delay);

                // Don't optimize away the timer for non-positive delays, as this might result in stack overflows.
                // We want the thread dispatch here.
                return Observable.timer(delay.toNanos(), NANOSECONDS).singleOrError().flatMap(timeout ->
                            repeat(single));
            });
    }

    private Duration calculateRefreshDelay(final Instant expiryTime) {
        final Duration durationUntilExpiry = Duration.between(clock.instant(), expiryTime);
        return durationUntilExpiry.isNegative()                                      //
            ? Duration.ZERO                                                          //
            : durationUntilExpiry.multipliedBy(settings.getRefreshTokenPercentage()) //
                                 .dividedBy(100);
    }
}
