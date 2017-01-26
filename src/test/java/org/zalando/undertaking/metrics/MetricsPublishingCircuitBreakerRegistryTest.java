package org.zalando.undertaking.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import io.github.robwin.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;

public class MetricsPublishingCircuitBreakerRegistryTest {

    private MetricsPublishingCircuitBreakerRegistry underTest;
    private CircuitBreakerRegistry delegate;
    private MetricRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new MetricRegistry();
        delegate = spy(new InMemoryCircuitBreakerRegistry(CircuitBreakerConfig.ofDefaults()));
        underTest = new MetricsPublishingCircuitBreakerRegistry(delegate, registry);
    }

    @Test
    public void delegatesGetAllCircuitBreakers() {
        underTest.getAllCircuitBreakers();
        verify(delegate).getAllCircuitBreakers();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void delegatesGetCircuitBreakerByName() {
        underTest.circuitBreaker("voldy");
        verify(delegate).circuitBreaker(eq("voldy"));
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void delegatesGetCircuitBreakerWithConfig() {
        CircuitBreakerConfig testConfig =
            CircuitBreakerConfig.custom()                 //
                                .failureRateThreshold(42) //
                                .build();
        underTest.circuitBreaker("voldy", testConfig);
        verify(delegate).circuitBreaker(eq("voldy"), eq(testConfig));
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void delegatesGetCircuitBreakerWithConfigSupplier() {
        Supplier<CircuitBreakerConfig> testSupplier =         //
            () ->
                CircuitBreakerConfig.custom()                 //
                                    .failureRateThreshold(42) //
                                    .build();

        underTest.circuitBreaker("voldy", testSupplier);
        verify(delegate).circuitBreaker(eq("voldy"), eq(testSupplier));
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void registersMetrics() {
        underTest.circuitBreaker("voldy");

        assertThat(registry.getMetrics()).describedAs("metrics registry") //
                                         .containsKey("circuitbreaker.voldy.timings");
    }

    @Test
    public void registersMetricsConfig() {
        CircuitBreakerConfig testConfig =
            CircuitBreakerConfig.custom()                 //
                                .failureRateThreshold(42) //
                                .build();
        underTest.circuitBreaker("voldy", testConfig);

        assertThat(registry.getMetrics()).describedAs("metrics registry") //
                                         .containsKey("circuitbreaker.voldy.timings");
    }

    @Test
    public void registersMetricsConfigSupplier() {
        Supplier<CircuitBreakerConfig> testSupplier =         //
            () ->
                CircuitBreakerConfig.custom()                 //
                                    .failureRateThreshold(42) //
                                    .build();

        underTest.circuitBreaker("voldy", testSupplier);

        assertThat(registry.getMetrics()).describedAs("metrics registry") //
                                         .containsKey("circuitbreaker.voldy.timings");
    }
}
