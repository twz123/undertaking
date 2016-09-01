package org.zalando.undertaking.metrics;

import javax.inject.Inject;
import javax.inject.Provider;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public final class MetricRegistryTimerProvider implements TimerProvider {

    private final MetricRegistry metricRegistry;
    private final Provider<Timer> timerProvider;

    @Inject
    public MetricRegistryTimerProvider(final MetricRegistry metricRegistry, final Provider<Timer> timerProvider) {
        this.metricRegistry = metricRegistry;
        this.timerProvider = timerProvider;
    }

    @Override
    public Timer get(final String metricName) {
        Timer timer = metricRegistry.getTimers().get(metricName);
        if (timer == null) {
            synchronized (metricRegistry) {
                timer = metricRegistry.getTimers().get(metricName);
                if (timer == null) {
                    timer = timerProvider.get();
                    metricRegistry.register(metricName, timer);
                }
            }
        }

        return timer;
    }

}
