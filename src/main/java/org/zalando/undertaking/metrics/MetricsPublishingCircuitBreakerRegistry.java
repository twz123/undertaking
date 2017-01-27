package org.zalando.undertaking.metrics;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

import javax.inject.Inject;

import com.codahale.metrics.MetricRegistry;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;

import javaslang.collection.Seq;

/**
 * All circuit breakers created by this registry will publish Metrics to the provided MetricsRegistry. Creation of the
 * actual circuit breakers is delegated to the provided CircuitBreakerRegistry
 *
 * @see  CircuitBreakerDropwizardMetrics Published Metrics per circuit breaker are:
 *
 *       <ul>
 *         <li>Circuit Breaker State</li>
 *         <li>Current Timestamp</li>
 *         <li>Failure Rate</li>
 *         <li># of buffered calls</li>
 *         <li># of failed calls</li>
 *         <li># of successful calls</li>
 *         <li># of calls that were not permitted due to the circuit breaker state</li>
 *         <li>max number of buffered calls</li>
 *         <li>request timings as `Histogram`</li>
 *       </ul>
 */
public class MetricsPublishingCircuitBreakerRegistry implements CircuitBreakerRegistry {
    private CircuitBreakerRegistry delegate;
    private MetricRegistry metricRegistry;

    /**
     * Creates an instance of this registry.
     *
     * @param  delegate        The actual circuit breaker registry to which this registry delegates circuit breaker
     *                         creation
     * @param  metricRegistry  The metric registry to which metrics are published.
     */
    @Inject
    public MetricsPublishingCircuitBreakerRegistry(final CircuitBreakerRegistry delegate,
            final MetricRegistry metricRegistry) {
        this.delegate = requireNonNull(delegate);
        this.metricRegistry = requireNonNull(metricRegistry);
    }

    @Override
    public Seq<CircuitBreaker> getAllCircuitBreakers() {
        return delegate.getAllCircuitBreakers();
    }

    @Override
    public CircuitBreaker circuitBreaker(final String name) {
        CircuitBreaker circuitBreaker = delegate.circuitBreaker(name);
        CircuitBreakerDropwizardMetrics.register(circuitBreaker, metricRegistry);

        return circuitBreaker;
    }

    @Override
    public CircuitBreaker circuitBreaker(final String name, final CircuitBreakerConfig circuitBreakerConfig) {
        CircuitBreaker circuitBreaker = delegate.circuitBreaker(name, circuitBreakerConfig);
        CircuitBreakerDropwizardMetrics.register(circuitBreaker, metricRegistry);

        return circuitBreaker;
    }

    @Override
    public CircuitBreaker circuitBreaker(final String name,
            final Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
        CircuitBreaker circuitBreaker = delegate.circuitBreaker(name, circuitBreakerConfigSupplier);
        CircuitBreakerDropwizardMetrics.register(circuitBreaker, metricRegistry);

        return circuitBreaker;
    }
}
