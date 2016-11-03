package org.zalando.undertaking.logbook;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RawHttpRequest;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;

public final class UndertowHttpRequest extends UndertowHttpMessage implements HttpRequest, RawHttpRequest {
    private Function<String, String> pathObfuscator;

    public UndertowHttpRequest(final HttpServerExchange exchange, final Function<String, String> pathObfuscator) {
        super(exchange);
        this.pathObfuscator = requireNonNull(pathObfuscator);
    }

    @Override
    public String getRemote() {
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    @Override
    public String getMethod() {
        return exchange.getRequestMethod().toString();
    }

    @Override
    public String getRequestUri() {
        final String queryString = exchange.getQueryString();
        final String path = pathObfuscator.apply(exchange.getRequestURI());
        return queryString != null && !queryString.isEmpty() ? path + '?' + queryString : path;
    }

    @Override
    public HttpRequest withBody() {
        return this;
    }

    @Override
    protected HeaderMap getExchangeHeaders() {
        return exchange.getRequestHeaders();
    }
}
