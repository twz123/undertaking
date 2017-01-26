package org.zalando.undertaking.metrics.guice;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.zalando.undertaking.metrics.DefaultMetricsResponderConfig;
import org.zalando.undertaking.metrics.MetricRegistryTimerProvider;
import org.zalando.undertaking.metrics.MetricsPublishingCircuitBreakerRegistry;
import org.zalando.undertaking.metrics.MetricsResponder;
import org.zalando.undertaking.metrics.PathTemplateBasedMetricsCollector;
import org.zalando.undertaking.metrics.PathTemplateBasedMetricsHandler;
import org.zalando.undertaking.metrics.TimerProvider;

import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;

import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;

public class SimpleHttpMetricsModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(CircuitBreakerRegistry.class).to(MetricsPublishingCircuitBreakerRegistry.class);
        bind(Clock.class).toProvider(Clock::defaultClock);
        bind(MetricFilter.class).toInstance(MetricFilter.ALL);

        bind(TimerProvider.class).to(MetricRegistryTimerProvider.class);
        bind(PathTemplateBasedMetricsCollector.class).in(Singleton.class);

        bind(PathTemplateBasedMetricsHandler.class);
        expose(PathTemplateBasedMetricsHandler.class);

        bind(MetricsResponder.Config.class).to(DefaultMetricsResponderConfig.class).in(Singleton.class);
        bind(MetricsResponder.class).in(Singleton.class);
        expose(MetricsResponder.class);
    }

    @Provides
    Timer provideTimer() {
        return new Timer(new SlidingTimeWindowReservoir(1, TimeUnit.MINUTES));
    }

}
