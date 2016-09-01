package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.MoreObjects.firstNonNull;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zalando.undertaking.inject.Request;
import org.zalando.undertaking.utils.FixedAttemptsStrategy;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import rx.Observable;
import rx.Single;

final class AuthenticationInfoProvider implements Provider<Single<AuthenticationInfo>> {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationInfoProvider.class);

    private final Single<AccessToken> accessToken;
    private final TokenInfoRequestProvider requestProvider;

    @Inject
    AuthenticationInfoProvider(@Request final Single<AccessToken> accessToken,
            final TokenInfoRequestProvider requestProvider) {
        this.accessToken = requireNonNull(accessToken);
        this.requestProvider = requireNonNull(requestProvider);
    }

    @Override
    public Single<AuthenticationInfo> get() {
        final String id = Long.toString(System.nanoTime(), Character.MAX_RADIX);
        return accessToken.flatMapObservable(requestProvider::toObservable)                                             //
                          .doOnError(error ->
                                  LOG.warn("Token info request [{}] failed: [{}]", id, error.getMessage()))             //
                          .single()                                                                                     //
                          .retry((attempt, error) ->
                                  new FixedAttemptsStrategy(3).shouldBeRetried(attempt, error))                         //
                          .doOnError(error ->
                                  LOG.warn("No further retries for token info request [{}]", id))                       //
                          .onErrorResumeNext(error -> Observable.error(unwrapHystrixBadRequest(error)))                 //
                          .cache()                                                                                      //
                          .toSingle();
    }

    private static Throwable unwrapHystrixBadRequest(final Throwable t) {
        return t instanceof HystrixBadRequestException ? firstNonNull(t.getCause(), t) : t;
    }

}
