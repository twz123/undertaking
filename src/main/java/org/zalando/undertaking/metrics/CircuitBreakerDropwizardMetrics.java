package org.zalando.undertaking.metrics;

import static java.util.Objects.requireNonNull;

import static javaslang.API.Case;
import static javaslang.API.Match;

import static javaslang.Predicates.instanceOf;

import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import com.google.common.collect.ImmutableList;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.robwin.circuitbreaker.event.CircuitBreakerOnIgnoredErrorEvent;
import io.github.robwin.circuitbreaker.event.CircuitBreakerOnSuccessEvent;

public class CircuitBreakerDropwizardMetrics {
    private static final List<Class> INTERESTING_EVENTS = ImmutableList.of( //
            CircuitBreakerOnSuccessEvent.class,                             //
            CircuitBreakerOnErrorEvent.class,                               //
            CircuitBreakerOnIgnoredErrorEvent.class);

    private static final String PREFIX = "circuitbreaker";

    private CircuitBreaker breaker;
    private MetricRegistry registry;

    private CircuitBreakerDropwizardMetrics(final CircuitBreaker breaker, final MetricRegistry registry) {
        this.breaker = requireNonNull(breaker);
        this.registry = requireNonNull(registry);
    }

    public static void register(final CircuitBreaker breaker, final MetricRegistry registry) {
        new CircuitBreakerDropwizardMetrics(breaker, registry).doRegister();
    }

    private void doRegister() {
        if (registry.getMetrics().containsKey(getPrefixedMetricName("state"))) {

            // Do not register or subscribe to the event stream more than once.
            return;
        }

        CircuitBreaker.Metrics metrics = breaker.getMetrics();

        registerGauge("state", () -> breaker.getState().toString());
        registerGauge("timestamp", System::currentTimeMillis);
        registerGauge("failureRate", metrics::getFailureRate);
        registerGauge("buffered", metrics::getNumberOfBufferedCalls);
        registerGauge("failed", metrics::getNumberOfFailedCalls);
        registerGauge("notPermitted", metrics::getNumberOfNotPermittedCalls);
        registerGauge("bufferedMax", metrics::getMaxNumberOfBufferedCalls);
        registerGauge("successful", metrics::getNumberOfSuccessfulCalls);

        Histogram timings = registry.histogram(getPrefixedMetricName("timings"));
        Meter requests = registry.meter(getPrefixedMetricName("requests"));

        breaker.getEventStream()                                                //
               .filter(ev -> INTERESTING_EVENTS.contains(ev.getClass()))        //
               .map(ev ->
                       Match(ev).of(                                            //
                           Case(instanceOf(CircuitBreakerOnSuccessEvent.class), //
                               CircuitBreakerOnSuccessEvent::getElapsedDuration), //
                           Case(instanceOf(CircuitBreakerOnErrorEvent.class),   //
                               CircuitBreakerOnErrorEvent::getElapsedDuration), //
                           Case(instanceOf(CircuitBreakerOnIgnoredErrorEvent.class), //
                               CircuitBreakerOnIgnoredErrorEvent::getElapsedDuration)) //
                       )                                                        //
               .doOnNext((duration) -> {                                        //
                   timings.update(duration.toMillis());                         //
                   requests.mark();                                             //
               })                                                               //
               .subscribe();
    }

    private <T> void registerGauge(final String name, final Gauge<T> fn) {
        registry.register(getPrefixedMetricName(name), fn);
    }

    private String getPrefixedMetricName(final String name) {
        String normalizedName = MetricNameNormalizer.normalize(breaker.getName());
        return PREFIX + "." + normalizedName + "." + name;
    }
}
