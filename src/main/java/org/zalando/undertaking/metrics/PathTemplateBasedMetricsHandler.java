package org.zalando.undertaking.metrics;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;

import io.undertow.UndertowOptions;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;

import io.undertow.util.PathTemplateMatch;

/**
 * Collects metrics based on the {@link PathTemplateMatch#ATTACHMENT_KEY path template} of a {@code HttpServerExchange}
 * and stores them using Dropwizard Metrics. Note that in order to work correctly, this class needs the
 * {@link UndertowOptions#RECORD_REQUEST_START_TIME} property to be enabled.
 *
 * @see  io.undertow.server.handlers.PathTemplateHandler
 */
public class PathTemplateBasedMetricsHandler implements HttpHandler {

    private final PathTemplateBasedMetricsCollector metricsCollector;

    private volatile HttpHandler next = ResponseCodeHandler.HANDLE_404;

    @Inject
    public PathTemplateBasedMetricsHandler(final PathTemplateBasedMetricsCollector metricsCollector) {
        this.metricsCollector = requireNonNull(metricsCollector);
    }

    public PathTemplateBasedMetricsHandler setNext(final HttpHandler next) {
        this.next = requireNonNull(next);
        return this;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.addExchangeCompleteListener(metricsCollector);
        next.handleRequest(exchange);
    }
}
