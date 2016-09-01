package org.zalando.undertaking.logbook;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zalando.logbook.Correlator;
import org.zalando.logbook.Logbook;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;

import io.undertow.util.AttachmentKey;

public class LogbookHandler implements HttpHandler, ExchangeCompletionListener {

    private static final Logger LOG = LoggerFactory.getLogger(LogbookHandler.class);

    private final Logbook logbook;

    private final AttachmentKey<Correlator> correlatorKey = AttachmentKey.create(Correlator.class);

    private volatile HttpHandler next = ResponseCodeHandler.HANDLE_404;

    @Inject
    public LogbookHandler(final Logbook logbook) {
        this.logbook = requireNonNull(logbook);
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
        final Optional<Correlator> correlator = logbook.write(new UndertowHttpRequest(exchange));
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
