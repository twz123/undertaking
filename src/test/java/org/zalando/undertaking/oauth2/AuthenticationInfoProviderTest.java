package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.zalando.undertaking.test.rx.hamcrest.TestSubscriberMatchers.hasOnlyError;
import static org.zalando.undertaking.test.rx.hamcrest.TestSubscriberMatchers.hasOnlyValue;

import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.COMMAND_EXCEPTION;
import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.REJECTED_SEMAPHORE_EXECUTION;
import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.TIMEOUT;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import rx.Observable;
import rx.Single;

import rx.observers.TestSubscriber;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationInfoProviderTest {

    private final Single<AccessToken> accessToken = Single.just(AccessToken.of("token"));

    @Mock
    private TokenInfoRequestProvider requestProvider;

    @Mock
    private AuthenticationInfo authenticationInfo;

    private AuthenticationInfoProvider underTest;

    @Before
    public void initializeTest() {
        underTest = new AuthenticationInfoProvider(Single.defer(() -> accessToken), requestProvider);
    }

    @Test
    public void emitsAuthenticationInfo() {
        final AccessToken token = accessToken.toBlocking().value();
        when(requestProvider.toObservable(token)).thenReturn(Observable.just(authenticationInfo));

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyValue(is(authenticationInfo)));

        verify(requestProvider).toObservable(token);
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void retriesTokenInfoCallOnCommandAndTimeoutExceptions() {
        when(requestProvider.toObservable(any())).thenReturn(Observable.error(makeHystrixException(COMMAND_EXCEPTION)))
                                                 .thenReturn(Observable.error(makeHystrixException(TIMEOUT)))
                                                 .thenReturn(Observable.just(authenticationInfo));

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyValue(is(authenticationInfo)));

        verify(requestProvider, times(3)).toObservable(any());
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void stopsRetryingAfterThreeAttempts() {
        final Throwable mockedFailure = makeHystrixException(COMMAND_EXCEPTION);
        when(requestProvider.toObservable(any())).thenReturn(Observable.error(mockedFailure));

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyError(is(mockedFailure)));

        verify(requestProvider, times(3)).toObservable(any());
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void noRetriesForOtherExceptions() {
        final Throwable mockedFailure = makeHystrixException(REJECTED_SEMAPHORE_EXECUTION);
        when(requestProvider.toObservable(any())).thenReturn(Observable.error(mockedFailure));

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyError(is(mockedFailure)));

        verify(requestProvider, times(1)).toObservable(any());
        verifyNoMoreInteractions(requestProvider);
    }

    private static Throwable makeHystrixException(final HystrixRuntimeException.FailureType type) {
        return new HystrixRuntimeException(type, null, "fail", null, null);
    }
}
