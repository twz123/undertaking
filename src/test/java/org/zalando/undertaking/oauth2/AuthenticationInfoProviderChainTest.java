package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Single;

import io.reactivex.observers.TestObserver;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationInfoProviderChainTest {

    @Mock
    private Provider<Single<AuthenticationInfo>> first, second;

    @Mock
    private AuthenticationInfo firstInfo, secondInfo;

    private Provider<Single<AuthenticationInfo>> underTest;

    private static <T> TestObserver<T> subscribeTo(final Single<T> single) {
        return single.test().awaitDone(30, TimeUnit.SECONDS);
    }

    @Before
    public void initializeTest() {
        underTest = new AuthenticationInfoProviderChain(Arrays.asList(first, second));
    }

    @Test
    public void emitsBadTokenInfoWithoutProviders() {
        final Single<AuthenticationInfo> single = new AuthenticationInfoProviderChain(Collections.emptyList()).get();

        final TestObserver<AuthenticationInfo> subscriber = subscribeTo(single);
        subscriber.assertNoValues();
        subscriber.assertError(BadTokenInfoException.class);
    }

    @Test
    public void emitsFirstIfBothAreSuccessful() {
        when(first.get()).thenReturn(Single.just(firstInfo));
        when(second.get()).thenReturn(Single.just(secondInfo));

        final TestObserver<AuthenticationInfo> subscriber = subscribeTo(underTest.get());
        subscriber.assertNoErrors();
        subscriber.assertValue(firstInfo);
    }

    @Test
    public void emitsSecondIfFirstFailsWithBadToken() {
        when(first.get()).thenReturn(Single.error(new BadTokenInfoException((String) null, (String) null)));
        when(second.get()).thenReturn(Single.just(secondInfo));

        final TestObserver<AuthenticationInfo> subscriber = subscribeTo(underTest.get());
        subscriber.assertNoErrors();
        subscriber.assertValue(secondInfo);
    }

    @Test
    public void emitsSecondIfFirstFailsWithMalformedToken() {
        when(first.get()).thenReturn(Single.error(new MalformedAccessTokenException()));
        when(second.get()).thenReturn(Single.just(secondInfo));

        final TestObserver<AuthenticationInfo> subscriber = subscribeTo(underTest.get());
        subscriber.assertNoErrors();
        subscriber.assertValue(secondInfo);
    }

    @Test
    public void failsIfFirstFailsWithGenericException() {
        final RuntimeException expected = new RuntimeException();
        when(first.get()).thenReturn(Single.error(expected));
        when(second.get()).thenReturn(Single.just(secondInfo));

        final TestObserver<AuthenticationInfo> subscriber = subscribeTo(underTest.get());
        subscriber.assertNoValues();
        subscriber.assertError(expected);
    }

    @Test
    public void emitsFirstIfSecondFailsWithGenericException() {
        when(first.get()).thenReturn(Single.just(firstInfo));
        when(second.get()).thenReturn(Single.error(new RuntimeException()));

        final TestObserver<AuthenticationInfo> subscriber = subscribeTo(underTest.get());
        subscriber.assertNoErrors();
        subscriber.assertValue(firstInfo);
    }

    @Test
    public void cachesResults() {
        final AtomicLong firstSubscribeTimesCalled = new AtomicLong(), secondSubscribeTimesCalled = new AtomicLong();
        final Single<AuthenticationInfo> firstSingle = Single.fromCallable(() -> {
                firstSubscribeTimesCalled.incrementAndGet();
                return firstInfo;
            });
        final Single<AuthenticationInfo> secondSingle = Single.fromCallable(() -> {
                secondSubscribeTimesCalled.incrementAndGet();
                return secondInfo;
            });
        when(first.get()).thenReturn(firstSingle);
        when(second.get()).thenReturn(secondSingle);

        final Single<AuthenticationInfo> single = underTest.get();

        final TestObserver<AuthenticationInfo> firstSubscriber = subscribeTo(single);
        firstSubscriber.assertNoErrors();
        firstSubscriber.assertValue(firstInfo);
        verify(first).get();
        verify(second).get();
        assertThat(firstSubscribeTimesCalled.get(), is(1L));
        assertThat(secondSubscribeTimesCalled.get(), lessThanOrEqualTo(1L));

        final TestObserver<AuthenticationInfo> secondSubscriber = subscribeTo(single);
        secondSubscriber.assertNoErrors();
        secondSubscriber.assertValue(firstInfo);
        verifyNoMoreInteractions(first, second);
        assertThat(firstSubscribeTimesCalled.get(), is(1L));
        assertThat(secondSubscribeTimesCalled.get(), lessThanOrEqualTo(1L));
    }
}
