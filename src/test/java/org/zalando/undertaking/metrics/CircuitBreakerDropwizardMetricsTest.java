package org.zalando.undertaking.metrics;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.assertj.core.api.AbstractObjectAssert;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;

public class CircuitBreakerDropwizardMetricsTest {
    private MetricRegistry metricRegistry;
    private CircuitBreaker defaultBreaker;
    private CircuitBreaker flimsyBreaker;

    private static final int DEFAULT_BREAKER_RINBGUFFER_SIZE = 4;

    @Before
    public void setUp() throws Exception {
        metricRegistry = new MetricRegistry();

        /* Usage of smaller ring buffer than the default so that the values that are computed only with a full ring
         * buffer
         * are available earlier */
        defaultBreaker = CircuitBreaker.of("defaultBreaker",
                CircuitBreakerConfig.custom().ringBufferSizeInClosedState(DEFAULT_BREAKER_RINBGUFFER_SIZE).build());

        /* Usage of an tiny ring buffer in order to easily trigger the circuit break */
        flimsyBreaker = CircuitBreaker.of("flimsyBreaker",
                CircuitBreakerConfig.custom().ringBufferSizeInClosedState(1).build());

        CircuitBreakerDropwizardMetrics.register(defaultBreaker, metricRegistry);
        CircuitBreakerDropwizardMetrics.register(flimsyBreaker, metricRegistry);
    }

    @Test
    public void publishesBreakerStateDefault() {
        assertGauge("circuitbreaker.defaultBreaker.state").isEqualTo("CLOSED");
    }

    @Test
    public void publishesBreakerStateOpen() {
        flimsyBreaker.onError(Duration.ofMillis(100), new Throwable());

        assertGauge("circuitbreaker.flimsyBreaker.state").isEqualTo("OPEN");
    }

    @Test
    public void publishesBreakerFailureRate() {
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onError(Duration.ofMillis(100), new Throwable());
        defaultBreaker.onError(Duration.ofMillis(100), new Throwable());

        assertGauge("circuitbreaker.defaultBreaker.failureRate").isEqualTo(50.0f);
    }

    @Test
    public void publishesBreakerBuffered() {
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));

        assertGauge("circuitbreaker.defaultBreaker.buffered").isEqualTo(2);
    }

    @Test
    public void publishesBreakerFailed() {
        defaultBreaker.onError(Duration.ofMillis(100), new Throwable());

        assertGauge("circuitbreaker.defaultBreaker.failed").isEqualTo(1);
    }

    @Test
    public void publishesBreakerNotPermitted() {
        flimsyBreaker.isCallPermitted();
        assertGauge("circuitbreaker.flimsyBreaker.notPermitted").isEqualTo(0L);

        flimsyBreaker.onError(Duration.ofMillis(100), new Throwable());

        flimsyBreaker.isCallPermitted();
        assertGauge("circuitbreaker.flimsyBreaker.notPermitted").isEqualTo(1L);
    }

    @Test
    public void publishesMaxBuffered() {
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));
        defaultBreaker.onSuccess(Duration.ofMillis(100));

        assertGauge("circuitbreaker.defaultBreaker.bufferedMax").isEqualTo(DEFAULT_BREAKER_RINBGUFFER_SIZE);
    }

    @Test
    public void publishesSuccessfulCalls() {
        defaultBreaker.onSuccess(Duration.ofMillis(100));

        assertGauge("circuitbreaker.defaultBreaker.successful").isEqualTo(1);
    }

    @Test
    public void publishesTimings() {
        defaultBreaker.onSuccess(Duration.ofMillis(123));

        Histogram histogram = metricRegistry.getHistograms(MetricFilter.ALL).get("circuitbreaker.defaultBreaker.timings");

        assertThat(histogram).as("histogram 'circuitbreaker.defaultBreaker.timings'").isNotNull();

        // Min is an arbitrary choice as we only care if *anything* was updated. We're not here to test Dropwizard
        // Metrics.
        assertThat(histogram.getSnapshot().getMin()).as("histogram 'circuitbreaker.defaultBreaker.timings' min value").isEqualTo(123);
    }

    @Test
    public void publishesRequestMeter() {
        defaultBreaker.onSuccess(Duration.ofMillis(123));
        defaultBreaker.onSuccess(Duration.ofMillis(123));
        defaultBreaker.onSuccess(Duration.ofMillis(123));

        Meter meter = metricRegistry.getMeters(MetricFilter.ALL).get("circuitbreaker.defaultBreaker.requests");

        assertThat(meter).as("meter 'circuitbreaker.defaultBreaker.requests'").isNotNull();

        // Same as above. Count is an arbitrary choice.
        assertThat(meter.getCount()).as("meter 'circuitbreaker.defaultBreaker.requests' count").isEqualTo(3);
    }

    @Test
    public void doesNotRegisterTwice() {

        // This should not throw anything, but still register default breaker
        CircuitBreakerDropwizardMetrics.register(defaultBreaker, metricRegistry);
        CircuitBreakerDropwizardMetrics.register(defaultBreaker, metricRegistry);

        assertGauge("circuitbreaker.defaultBreaker.state").isEqualTo("CLOSED");
    }

    private AbstractObjectAssert<?, Object> assertGauge(final String key) {
        Gauge gauge = metricRegistry.getGauges(MetricFilter.ALL).get(key);

        assertThat(gauge).as("gauge named " + key).isNotNull();
        return assertThat(gauge.getValue()).as("value of gauge named '" + key + "'");
    }
}
