package org.zalando.undertaking.inject.rx.guice;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.inject.rx.RxHttpExchangeScope;

import io.undertow.server.HttpServerExchange;

final class InternalRxHttpExchangeScope implements RxHttpExchangeScope {

    private final HttpExchangeScope delegate;

    @Inject
    public InternalRxHttpExchangeScope(final HttpExchangeScope delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <X extends Throwable> void runScoped(final HttpServerExchange exchange,
            final HttpExchangeScope.ScopedAction<X> action) throws X {
        delegate.runScoped(exchange, action);
    }

}
