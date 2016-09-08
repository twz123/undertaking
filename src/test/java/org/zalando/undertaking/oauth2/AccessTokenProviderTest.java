package org.zalando.undertaking.oauth2;

import static java.time.temporal.ChronoUnit.DAYS;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.ConnectException;

import java.time.Clock;
import java.time.Instant;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import org.zalando.undertaking.oauth2.credentials.ClientCredentials;
import org.zalando.undertaking.oauth2.credentials.RequestCredentials;
import org.zalando.undertaking.oauth2.credentials.UserCredentials;

import rx.Observable;
import rx.Single;
import rx.Subscription;

import rx.functions.Action0;
import rx.functions.Action1;

import rx.subjects.AsyncSubject;
import rx.subjects.PublishSubject;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokenProviderTest {

    private final Single<RequestCredentials> requestCredentials = Single.just(new RequestCredentials(
                new ClientCredentials(), new UserCredentials()));

    @Mock
    private AccessTokenRequestProvider requestProvider;

    @Mock
    private AccessTokenSettings settings;

    @Mock
    private Clock clock;

    private AccessTokenProvider underTest;

    @Before
    public void setUp() {
        when(settings.getRefreshTokenPercentage()).thenReturn(100);
        when(clock.instant()).thenReturn(Instant.EPOCH);

        underTest = new AccessTokenProvider(Single.defer(() -> requestCredentials), requestProvider, settings, clock);
    }

    @Test(timeout = 30000)
    public void automaticallyRefreshesAccessToken() {
        final PublishSubject<AccessTokenResponse> input = PublishSubject.create();
        final PublishSubject<Void> consumed = PublishSubject.create();
        final RequestCredentials credentials = requestCredentials.toBlocking().value();
        final Action0 notifyConsumed = () ->
                Observable.timer(20, TimeUnit.MILLISECONDS).subscribe(triggered -> consumed.onNext(null));

        when(requestProvider.requestAccessToken(credentials)).thenReturn(input.take(1).doOnTerminate(notifyConsumed)
                .toSingle());

        // Use the same Single instance twice, since we're using it as a Singleton inside OAuth2Module.
        // We need to ensure that the same instance yields current results when subscribed to.
        final Single<AccessToken> single = underTest.get();

        doDuringAutoUpdate(() -> {
            input.onNext(new AccessTokenResponse(AccessToken.of("first"), Instant.EPOCH));
            consumed.take(1).toCompletable().await();
            assertThat(single.toBlocking().value(), is(AccessToken.of("first")));

            input.onNext(new AccessTokenResponse(AccessToken.of("second"), Instant.EPOCH.plus(1, DAYS)));
            consumed.take(1).toCompletable().await();
            assertThat(single.toBlocking().value(), is(AccessToken.of("second")));

            verify(requestProvider, times(2)).requestAccessToken(credentials);
            verifyNoMoreInteractions(requestProvider);
        });
    }

    @Test(timeout = 30000)
    public void restartsThresholdProviderAfterSuccessfulRequest() {
        final AccessTokenResponse first = new AccessTokenResponse(AccessToken.of("first"), Instant.EPOCH);
        final AccessTokenResponse second = new AccessTokenResponse(AccessToken.of("second"),
                Instant.EPOCH.plus(1, DAYS));

        final AsyncSubject<Void> consumed = AsyncSubject.create();
        final Action1<? super AccessTokenResponse> notifyConsumed = token ->
                Observable.timer(20, MILLISECONDS).doOnTerminate(() -> consumed.onCompleted()).subscribe();

        when(requestProvider.requestAccessToken(any())).thenReturn(connectError("first time"))                    //
                                                       .thenReturn(connectError("0 sec retry after first time"))  //
                                                       .thenReturn(Single.just(first))                            //
                                                       .thenReturn(connectError("second time"))                   //
                                                       .thenReturn(connectError("0 sec retry after second time")) //
                                                       .thenReturn(Single.just(second).doOnSuccess(notifyConsumed));

        final Single<AccessToken> single = underTest.get();

        doDuringAutoUpdate(() -> {
            assertThat(single.toBlocking().value(), is(AccessToken.of("first")));

            consumed.toCompletable().await();
            assertThat(single.toBlocking().value(), is(AccessToken.of("second")));

            verify(requestProvider, times(6)).requestAccessToken(any());
        });
    }

    private void doDuringAutoUpdate(final Runnable action) {
        final Subscription updateSubscription = underTest.autoUpdate();
        try {
            action.run();
        } finally {
            updateSubscription.unsubscribe();
        }
    }

    private static <T> Single<T> connectError(final String message) {
        return Single.error(new ConnectException(message));
    }
}
