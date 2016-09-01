package org.zalando.undertaking.logbook;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RawHttpRequest;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;

public final class UndertowHttpRequest extends UndertowHttpMessage implements HttpRequest, RawHttpRequest {

    public UndertowHttpRequest(final HttpServerExchange exchange) {
        super(exchange);
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
        final String uri = exchange.getRequestURI();
        return queryString != null && !queryString.isEmpty() ? uri + '?' + queryString : uri;
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
