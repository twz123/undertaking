package org.zalando.undertaking.logbook;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zalando.logbook.Correlator;
import org.zalando.logbook.Logbook;

import com.google.common.base.MoreObjects;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;

import io.undertow.util.AttachmentKey;

public class LogbookHandler implements HttpHandler, ExchangeCompletionListener {

    private static final Logger LOG = LoggerFactory.getLogger(LogbookHandler.class);

    private final Logbook logbook;
    private final Function<String, String> pathObfuscator;

    private final AttachmentKey<Correlator> correlatorKey = AttachmentKey.create(Correlator.class);

    private volatile HttpHandler next = ResponseCodeHandler.HANDLE_404;

    @Inject
    public LogbookHandler(final Logbook logbook,
            @Nullable
            @Named("logbook.pathObfuscator")
            final Function<String, String> pathObfuscator) {
        this.logbook = requireNonNull(logbook);
        this.pathObfuscator = MoreObjects.firstNonNull(pathObfuscator, Function.identity());
    }

    public void setNext(final HttpHandler next) {
        this.next = requireNonNull(next);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        logRequest(exchange);
        next.handleRequest(exchange);
    }

    private void logRequest(final HttpServerExchange exchange) throws IOException {
        final Optional<Correlator> correlator = logbook.write(new UndertowHttpRequest(exchange, pathObfuscator));
        if (correlator.isPresent()) {
            exchange.putAttachment(correlatorKey, correlator.get());
            exchange.addExchangeCompleteListener(this);
        }
    }

    @Override
    public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
        try {
            exchange.getAttachment(correlatorKey).write(new UntertowHttpResponse(exchange));
        } catch (final IOException e) {
            LOG.error("Failed to log HTTP response: [{}]", e.getMessage(), e);
        } finally {
            nextListener.proceed();
        }
    }
}
