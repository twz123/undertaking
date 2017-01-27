package org.zalando.undertaking.metrics;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;

import com.google.common.base.Ascii;

import io.undertow.UndertowOptions;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.PathTemplateMatch;

/**
 * Collects metrics based on the {@link PathTemplateMatch#ATTACHMENT_KEY path template} of a {@code HttpServerExchange}
 * and stores it into Dropwizard {@link Timer Timers} obtained from a {@link TimerProvider}. Note that in order to work
 * correctly, this class needs the {@link UndertowOptions#RECORD_REQUEST_START_TIME} property to be enabled.
 *
 * @see  io.undertow.server.handlers.PathTemplateHandler
 */
public final class PathTemplateBasedMetricsCollector implements ExchangeCompletionListener {
    private static final Logger LOG = LoggerFactory.getLogger(PathTemplateBasedMetricsCollector.class);

    private final TimerProvider timerProvider;
    private final Clock clock;

    @Inject
    public PathTemplateBasedMetricsCollector(final TimerProvider timerProvider, final Clock clock) {
        this.timerProvider = requireNonNull(timerProvider);
        this.clock = requireNonNull(clock);
    }

    @Override
    public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
        try {
            recordMetrics(exchange);
        } finally {
            nextListener.proceed();
        }
    }

    private void recordMetrics(final HttpServerExchange exchange) {
        final long requestStartTime = exchange.getRequestStartTime();

        if (requestStartTime < 0) {
            LOG.warn("Could not record metrics for [{}]: request start time has not been stored", exchange);
            return;
        }

        final int statusCode = exchange.getStatusCode();
        final String requestMethod = Ascii.toUpperCase(exchange.getRequestMethod().toString());
        final String metricSuffix = getMetricSuffix(exchange);

        final long duration = clock.getTick() - requestStartTime;

        LOG.debug("Submitting [{}µs] for [{} {} {}]", duration / 1000L, statusCode, requestMethod, metricSuffix);

        submitToTimer(statusCode, requestMethod, metricSuffix, duration);
    }

    private void submitToTimer(final int statusCode, final String requestMethod, final String metricSuffix,
            final long durationNanos) {

        // ZNON always expects at least two parts: status code and request method

        final String statusCodeString = Integer.toString(statusCode);
        submitToTimer(statusCodeString + '.' + requestMethod + '.' + metricSuffix, durationNanos);    // e.g. 200.GET.health
        submitToTimer(statusCodeString + ".ALL", durationNanos);                                      // e.g. 200.ALL
        submitToTimer((statusCode / 100) + "XX.ALL", durationNanos);                                  // e.g. 4XX.ALL
        submitToTimer("ALL.ALL", durationNanos);                                                      // e.g. ALL.ALL haha!
    }

    private void submitToTimer(final String metricName, final long durationNanos) {
        final String prefixedMetricName = "zmon.response." + metricName;
        try {
            timerProvider.get(prefixedMetricName).update(durationNanos, TimeUnit.NANOSECONDS);
        } catch (final Exception e) {
            LOG.warn("Unable to submit timer metric [{}]", prefixedMetricName, e);
        }
    }

    private String getMetricSuffix(final HttpServerExchange exchange) {
        final String path = getPath(exchange);
        if (path == null) {
            return "unmapped";
        }

        if (path.length() == 1 && path.charAt(0) == '/') {
            return "root";
        }

        return MetricNameNormalizer.normalize(path);
    }

    private static String getPath(final HttpServerExchange exchange) {
        final PathTemplateMatch match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        return match == null ? null : match.getMatchedTemplate();
    }
}
