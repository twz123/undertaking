package org.zalando.undertaking.inject;

import static com.google.common.base.Preconditions.checkNotNull;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Runs code in the context of a specific {@link HttpServerExchange} in respect to {@link HttpExchangeScoped}. Code that
 * is being executed through instances of this interface will receive injections based on the specified
 * {@code HttpServerExchange}.
 */
@FunctionalInterface
public interface HttpExchangeScope {

    /**
     * A unit of work that shall be executed within the scope of a specific {@code HttpServerExchange}.
     *
     * @param  <X>  exception type that can be produced by this action
     */
    @FunctionalInterface
    interface ScopedAction<X extends Throwable> {
        void execute() throws X;
    }

    /**
     * Executes the given {@code action} in the scope of the given {@code HttpServerExchange}.
     *
     * @param   exchange {@code HttpServerExchange} instance inside which the {@code action} shall be executed
     * @param   action    unit of work to be executed in the scope of the given {@code HttpServerExchange}
     * @param   <X>       exception type that can be produced by the given {@code action}
     *
     * @throws  X                     if the execution of {@code action} threw an exception of this type
     * @throws  NullPointerException  if at least one of the parameters is {@code null}
     */
    <X extends Throwable> void runScoped(final HttpServerExchange exchange, final ScopedAction<X> action) throws X;

    /**
     * Wraps a {@code HttpHandler} so that it runs in the scope of the {@code HttpServerExchange} passed to it.
     *
     * @param   wrapped {@code HttpHandler} that shall be wrapped
     *
     * @return  a wrapping {@code HttpHandler} that scopes the execution of the wrapped {@code HttpHandler} to the
     *          actual {@code HttpServerExchange} that gets passed to it
     *
     * @throws  NullPointerException  if {@code wrapped} is {@code null}
     */
    default HttpHandler scoped(final HttpHandler wrapped) {
        checkNotNull(wrapped);
        return exchange -> runScoped(exchange, () -> wrapped.handleRequest(exchange));
    }

    /**
     * Creates a {@code HttpExchangeScopedExecutor} from this scope and the given {@code exchange}.
     *
     * @param   exchange {@code HttpServerExchange} to be used to scope executions of the returned executor to
     *
     * @return  an executor that will scope units of work to the given {@code exchange}
     *
     * @throws  NullPointerException  if {@code exchange} is {@code null}
     */
    default HttpExchangeScopedExecutor createExecutorFor(final HttpServerExchange exchange) {
        checkNotNull(exchange);
        return new HttpExchangeScopedExecutor() {
            @Override
            public <X extends Throwable> void runScoped(final ScopedAction<X> action) throws X {
                HttpExchangeScope.this.runScoped(exchange, action);
            }
        };
    }
}
