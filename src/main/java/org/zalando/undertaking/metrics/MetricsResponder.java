package org.zalando.undertaking.metrics;

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.Headers;

/**
 * Responds with the metrics in a given registry as an {@code application/json} response.
 */
public class MetricsResponder implements HttpHandler {

    /**
     * Configuration values for a {@code MetricResponder}.
     */
    public interface Config {

        /**
         * @return  the {@link TimeUnit} to which rates should be converted.
         */
        default TimeUnit getRateUnit() {
            return TimeUnit.MINUTES;
        }

        /**
         * @return  the {@link TimeUnit} to which durations should be converted.
         */
        default TimeUnit getDurationUnit() {
            return TimeUnit.MILLISECONDS;
        }

        /**
         * @return  {@code true} if the samples shall be included in responses, {@code false} otherwise.
         */
        default boolean showSamples() {
            return false;
        }

    }

    private static final String CONTENT_TYPE = "application/json";

    private final MetricRegistry registry;
    private final ObjectMapper mapper;

    @Inject
    public MetricsResponder(final MetricRegistry registry, final MetricFilter filter, final Config config) {
        this.registry = requireNonNull(registry);
        this.mapper = new ObjectMapper().registerModule(new MetricsModule(config.getRateUnit(),
                    config.getDurationUnit(), config.showSamples(), filter));
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();

        try(final OutputStream output = exchange.getOutputStream()) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, CONTENT_TYPE);
            mapper.writer().writeValue(output, registry);
        }
    }
}
