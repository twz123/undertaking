package org.zalando.undertaking.oauth2;

import static java.time.temporal.ChronoUnit.DAYS;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.ConnectException;

import java.time.Clock;
import java.time.Instant;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import org.zalando.undertaking.oauth2.credentials.ClientCredentials;
import org.zalando.undertaking.oauth2.credentials.RequestCredentials;
import org.zalando.undertaking.oauth2.credentials.UserCredentials;

import io.reactivex.Single;

import io.reactivex.functions.BiPredicate;

import io.reactivex.observers.TestObserver;

import io.reactivex.plugins.RxJavaPlugins;

import io.reactivex.schedulers.TestScheduler;

@SuppressWarnings("unchecked")
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

    private TestScheduler testScheduler;
    private AccessTokenProvider underTest;

    @Before
    public void setUp() {
        when(settings.getRefreshTokenPercentage()).thenReturn(100);
        when(clock.instant()).thenReturn(Instant.EPOCH);

        underTest = new AccessTokenProvider(Single.defer(() -> requestCredentials), requestProvider, settings, clock);

        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler((s) -> testScheduler);
    }

    @After
    public void tearDown() {
        RxJavaPlugins.reset();
    }

    @Test
    public void waitsIfGetOccursBeforeUpdate() {
        final RequestCredentials credentials = requestCredentials.blockingGet();

        when(requestProvider.requestAccessToken(credentials)).thenReturn(tokenResponse("first", Instant.EPOCH));

        TestObserver<AccessToken> test = underTest.get().test();

        underTest.update().subscribe();
        testScheduler.triggerActions();
        test.assertValue(AccessToken.bearer("first"));
    }

    @Test
    public void throwsIfGetOccursBeforeUpdateAndUpdateNeverArrives() {
        TestObserver<AccessToken> test = underTest.get().test();

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        test.assertError(TimeoutException.class);
    }

    @Test
    public void automaticallyRefreshesAccessToken() {
        final RequestCredentials credentials = requestCredentials.blockingGet();

        when(requestProvider.requestAccessToken(credentials)).thenReturn(tokenResponse("first", Instant.EPOCH),
            tokenResponse("second", Instant.EPOCH.plus(1, DAYS)), tokenResponse("third", Instant.EPOCH.plus(1, DAYS)));
        underTest.autoUpdate();

        Single<AccessToken> accessTokenSingle = underTest.get();

        // Got expired token ('first'); instantly requests new token and gets 'second'
        testScheduler.triggerActions();
        accessTokenSingle.test().assertValue(AccessToken.bearer("second"));

        // 'second' is set to expire in one day; so after one day passes, the third token should be requested.
        testScheduler.advanceTimeBy(1, TimeUnit.DAYS);
        accessTokenSingle.test().assertValue(AccessToken.bearer("third"));

        verify(requestProvider, times(3)).requestAccessToken(credentials);
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void restartsThresholdProviderAfterSuccessfulRequest() {
        when(requestProvider.requestAccessToken(any())).thenReturn(errorResponse("first time"),
            errorResponse("0 sec retry after first time"), tokenResponse("first", Instant.EPOCH.plusSeconds(2)),
            errorResponse("second time"), errorResponse("0 sec retry after second time"),
            tokenResponse("second", Instant.EPOCH.plus(1, DAYS)));

        underTest.autoUpdate();

        BiPredicate<Integer, Throwable> ignoreTimeouts = (count, error) -> error instanceof TimeoutException;

        // The initial two calls fail
        testScheduler.triggerActions();

        final Single<AccessToken> single = underTest.get();

        // Timeout is now set to 1s; After that, the third call succeeds
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        single.retry(ignoreTimeouts).test().assertValue(AccessToken.bearer("first"));

        // 'first' is valid for 2s (wow).
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        // The next two calls fail, timeout is now set to 1s
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        // Great success
        single.retry(ignoreTimeouts).test().assertValue(AccessToken.bearer("second"));

        verify(requestProvider, times(6)).requestAccessToken(any());
    }

    private <T> Single<T> errorResponse(final String message) {
        return Single.error(new ConnectException(message));
    }

    private Single<AccessTokenResponse> tokenResponse(final String value, final Instant time) {
        return Single.just(new AccessTokenResponse(AccessToken.bearer(value), time));
    }
}
