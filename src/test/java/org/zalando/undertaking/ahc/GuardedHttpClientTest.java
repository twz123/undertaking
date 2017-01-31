package org.zalando.undertaking.ahc;

import static org.assertj.core.api.Java6Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.net.SocketTimeoutException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;

import io.reactivex.Single;
import io.reactivex.SingleObserver;

import io.reactivex.observers.TestObserver;

import io.reactivex.plugins.RxJavaPlugins;

import io.reactivex.schedulers.TestScheduler;

public class GuardedHttpClientTest {
    private GuardedHttpClient underTest;
    private BoundRequestBuilder boundRequestBuilder;
    private TestScheduler testScheduler;
    private ClientConfig.Builder defaultBuilder;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private Single<Response> single;

    @Before
    public void setUp() throws Exception {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        underTest = new GuardedHttpClient(circuitBreakerRegistry, (r) -> single);
        boundRequestBuilder = mock(BoundRequestBuilder.class);
        testScheduler = new TestScheduler();
        defaultBuilder = ClientConfig.builder().circuitBreakerName("testBreaker");
    }

    @Test
    public void success() {
        single = spy(Single.just(mock(Response.class)));

        ClientConfig config = defaultBuilder.maxRetries(5)
                                            .nonRetryableExceptions(ImmutableSet.of(TimeoutException.class)).build();

        underTest.executeRequest(boundRequestBuilder, staticMessage(), config).test().assertValue("completed");

        verifyNumberOfSubscribes(single, 1);
    }

    @Test
    public void retries() {
        single = spy(Single.error(new SocketTimeoutException()));

        ClientConfig config = defaultBuilder.maxRetries(5).build();

        underTest.executeRequest(boundRequestBuilder, staticMessage(), config).test().assertError(
            SocketTimeoutException.class);

        // 1 initial call and 5 retries = 6 subscribes
        verifyNumberOfSubscribes(single, 6);
    }

    @Test
    public void doesNotRetryNonRetryable() {
        single = spy(Single.error(new NullPointerException()));

        ClientConfig config = defaultBuilder.maxRetries(5)
                                            .nonRetryableExceptions(ImmutableSet.of(NullPointerException.class))
                                            .build();

        underTest.executeRequest(boundRequestBuilder, staticMessage(), config).test().assertError(
            NullPointerException.class);

        verifyNumberOfSubscribes(single, 1);
    }

    @Test
    public void timesOut() {
        RxJavaPlugins.setComputationSchedulerHandler((s) -> testScheduler);

        single = spy(Single.never());

        ClientConfig config = defaultBuilder.maxRetries(5).timeOutMs(5000)
                                            .nonRetryableExceptions(ImmutableSet.of(TimeoutException.class)).build();

        testScheduler.triggerActions();

        TestObserver<String> testObserver = underTest.executeRequest(boundRequestBuilder, staticMessage(), config)
                                                     .test();

        testScheduler.advanceTimeBy(5000, TimeUnit.MILLISECONDS);
        testObserver.assertError(TimeoutException.class);

        verifyNumberOfSubscribes(single, 1);
    }

    @Test
    public void engagesCircuitBreaker() {
        single = spy(Single.error(new NullPointerException()));

        ClientConfig config = defaultBuilder.maxRetries(5)
                                            .nonRetryableExceptions(ImmutableSet.of(NullPointerException.class))
                                            .build();

        underTest.executeRequest(boundRequestBuilder, staticMessage(), config).test().assertError(
            NullPointerException.class);

        verifyNumberOfSubscribes(single, 1);
        assertThat(circuitBreakerMetrics("testBreaker").getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void doesNotEngageCircuitBreakerMetrics() {
        single = spy(Single.error(new NullPointerException()));

        ClientConfig config = defaultBuilder.maxRetries(5)
                                            .nonRetryableExceptions(ImmutableSet.of(NullPointerException.class))
                                            .circuitBreakerIgnoreFailures(ImmutableSet.of(NullPointerException.class))
                                            .build();

        underTest.executeRequest(boundRequestBuilder, staticMessage(), config).test().assertError(
            NullPointerException.class);

        verifyNumberOfSubscribes(single, 1);
        assertThat(circuitBreakerMetrics("testBreaker").getNumberOfFailedCalls()).isEqualTo(0);
    }

    private CircuitBreaker.Metrics circuitBreakerMetrics(final String name) {
        return circuitBreakerRegistry.circuitBreaker(name).getMetrics();
    }

    private Function<Response, String> staticMessage() {
        return (r) -> "completed";
    }

    private Single<Response> mockHttpResponse(Single<Response> single) {
        single = spy(single);
        return single;
    }

    @SuppressWarnings("unchecked")
    private void verifyNumberOfSubscribes(final Single<?> single, final int wantedNumberOfInvocations) {
        verify(single, times(wantedNumberOfInvocations)).subscribe(any(SingleObserver.class));
    }
}
