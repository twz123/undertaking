package org.zalando.undertaking.inject;

import java.util.concurrent.Executor;

import io.undertow.server.HttpServerExchange;

/**
 * An executor that runs units of action in the calling thread, scoped to the {@link HttpServerExchange} that is
 * attached to the interface's instance.
 */
public interface HttpExchangeScopedExecutor extends Executor {

    <X extends Throwable> void runScoped(final HttpExchangeScope.ScopedAction<X> action) throws X;

    /**
     * Executes the given {@code action} in the calling thread, scoped to this instance's {@code HttpServerExchange}.
     *
     * @param   action  unit of work that is to be executed inside this instances {@code HttpServerExchange} scope
     *
     * @throws  NullPointerException  if {@code action} is {@code null}
     */
    default @Override void execute(final Runnable action) {
        runScoped(action::run);
    }

}
