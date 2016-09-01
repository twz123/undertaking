package org.zalando.undertaking.metrics;

import com.codahale.metrics.Timer;

public interface TimerProvider {

    Timer get(String metricName);

}
