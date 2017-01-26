package org.zalando.undertaking.logbook;

import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.RawHttpResponse;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;

public final class UndertowHttpResponse extends UndertowHttpMessage implements HttpResponse, RawHttpResponse {

    public UndertowHttpResponse(final HttpServerExchange exchange) {
        super(exchange);
    }

    @Override
    public int getStatus() {
        return exchange.getStatusCode();
    }

    @Override
    public HttpResponse withBody() {
        return this;
    }

    @Override
    protected HeaderMap getExchangeHeaders() {
        return exchange.getResponseHeaders();
    }
}
