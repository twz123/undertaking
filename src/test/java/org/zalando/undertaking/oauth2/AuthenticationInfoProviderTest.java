package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.zalando.undertaking.test.rx.hamcrest.TestSubscriberMatchers.hasOnlyError;
import static org.zalando.undertaking.test.rx.hamcrest.TestSubscriberMatchers.hasOnlyValue;

import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.COMMAND_EXCEPTION;
import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.REJECTED_SEMAPHORE_EXECUTION;
import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.TIMEOUT;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import io.undertow.util.HeaderMap;

import rx.Observable;
import rx.Single;

import rx.observers.TestSubscriber;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationInfoProviderTest {

    @Mock
    private Provider<Single<AccessToken>> accessTokenProvider;

    @Mock
    private Provider<HeaderMap> requestHeadersProvider;

    @Mock
    private TokenInfoRequestProvider requestProvider;

    @Mock
    private AuthenticationInfo authenticationInfo;

    private AuthenticationInfoProvider underTest;

    private final AccessToken accessToken = AccessToken.of("token");

    private final HeaderMap requestHeaders = new HeaderMap();

    @Before
    public void initializeTest() {
        when(accessTokenProvider.get()).thenReturn(Single.just(accessToken));
        when(requestHeadersProvider.get()).thenReturn(requestHeaders);

        underTest = new AuthenticationInfoProvider(accessTokenProvider, requestHeadersProvider, requestProvider);
    }

    @Test
    public void callsEndpointOnlyOnce() {
        doReturn(mockSuccess(authenticationInfo)).when(requestProvider).createCommand(any(), any());

        final TestSubscriber<AuthenticationInfo> first = new TestSubscriber<>();
        final Single<AuthenticationInfo> single = underTest.get();
        single.subscribe(first);

        first.awaitTerminalEvent();
        assertThat(first, hasOnlyValue(is(authenticationInfo)));

        verify(requestProvider).createCommand(accessToken, requestHeaders);
        verifyNoMoreInteractions(requestProvider);

        final TestSubscriber<AuthenticationInfo> second = new TestSubscriber<>();
        single.subscribe(second);

        second.awaitTerminalEvent();
        assertThat(second, hasOnlyValue(is(authenticationInfo)));
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void retriesTokenInfoCallOnCommandAndTimeoutExceptions() {

        //J-
        doReturn(
           mockError(makeHystrixException(COMMAND_EXCEPTION)),
           mockError(makeHystrixException(TIMEOUT)),
           mockSuccess(authenticationInfo)
        ).when(requestProvider).createCommand(any(), any());
        //J+

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyValue(is(authenticationInfo)));

        verify(requestProvider, times(3)).createCommand(any(), any());
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void stopsRetryingAfterThreeAttempts() {
        final Throwable mockedFailure = makeHystrixException(COMMAND_EXCEPTION);
        doReturn(mockError(mockedFailure)).when(requestProvider).createCommand(any(), any());

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyError(is(mockedFailure)));

        verify(requestProvider, times(3)).createCommand(any(), any());
        verifyNoMoreInteractions(requestProvider);
    }

    @Test
    public void noRetriesForOtherExceptions() {
        final Throwable mockedFailure = makeHystrixException(REJECTED_SEMAPHORE_EXECUTION);
        doReturn(mockError(mockedFailure)).when(requestProvider).createCommand(any(), any());

        final TestSubscriber<AuthenticationInfo> subscriber = new TestSubscriber<>();
        underTest.get().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        assertThat(subscriber, hasOnlyError(is(mockedFailure)));

        verify(requestProvider, times(1)).createCommand(any(), any());
        verifyNoMoreInteractions(requestProvider);
    }

    private static <T> HystrixObservableCommand<T> mockSuccess(final T result) {
        return mockCommand(Observable.just(result));
    }

    private static <T> HystrixObservableCommand<T> mockError(final Throwable error) {
        return mockCommand(Observable.error(error));
    }

    private static <T> HystrixObservableCommand<T> mockCommand(final Observable<T> delegate) {
        final HystrixObservableCommand<T> mock = mock(HystrixObservableCommand.class, RETURNS_DEEP_STUBS);
        doReturn(delegate).when(mock).toObservable();
        return mock;
    }

    private static Throwable makeHystrixException(final HystrixRuntimeException.FailureType type) {
        return new HystrixRuntimeException(type, null, "fail", null, null);
    }
}
