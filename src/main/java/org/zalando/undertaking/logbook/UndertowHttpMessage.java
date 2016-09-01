package org.zalando.undertaking.logbook;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Optional;

import org.zalando.logbook.HttpMessage;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.MediaType;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

public abstract class UndertowHttpMessage implements HttpMessage {

    private static final byte[] EMPTY = new byte[0];

    protected final HttpServerExchange exchange;

    protected UndertowHttpMessage(final HttpServerExchange exchange) {
        this.exchange = requireNonNull(exchange);
    }

    @Override
    public Multimap<String, String> getHeaders() {
        final ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
        for (final HeaderValues values : getExchangeHeaders()) {
            builder.putAll(values.getHeaderName().toString(), values);
        }

        return builder.build();
    }

    @Override
    public String getContentType() {
        final HeaderValues values = getExchangeHeaders().get(Headers.CONTENT_TYPE);
        return values == null ? "" : values.getFirst();
    }

    @Override
    public Charset getCharset() {
        return
            Optional.ofNullable(getExchangeHeaders().get(Headers.CONTENT_TYPE)) //
                    .map(HeaderValues::getFirst)                                //
                    .map(MediaType::parse)                                      //
                    .map(type -> type.charset().orNull())                       //
                    .orElse((StandardCharsets.UTF_8));
    }

    @Override
    public byte[] getBody() {
        return EMPTY;
    }

    @Override
    public String getBodyAsString() {
        return "";
    }

    protected abstract HeaderMap getExchangeHeaders();

}
