package org.zalando.undertaking.handlers;

import static java.util.Objects.requireNonNull;

import io.undertow.server.HttpHandler;

/**
 * Static utility methods that create commonly used {@link HttpHandler HttpHandlers}.
 */
public final class MoreHandlers {

    /**
     * Ends the {@code HttpServerExchange}.
     *
     * @see  io.undertow.server.HttpServerExchange#endExchange()
     */
    public static HttpHandler endExchange() {
        return exchange -> exchange.endExchange();
    }

    /**
     * Ends the {@code HttpServerExchange} after the given {@code handler} returns.
     *
     * @throws  NullPointerException  if {@code handler} is {@code null}
     *
     * @see     io.undertow.server.HttpServerExchange#endExchange()
     */
    public static HttpHandler endExchangeAfter(final HttpHandler handler) {
        requireNonNull(handler);

        return exchange -> {
            handler.handleRequest(exchange);
            exchange.endExchange();
        };
    }

    private MoreHandlers() {
        throw new AssertionError("No instances for you!");
    }
}
