package org.zalando.undertaking.inject.guice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.zalando.undertaking.inject.HttpExchangeScope;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.AttachmentKey;

/**
 * Guice implementation to support scoping to {@code HttpServerExchange} and implementation of {@code HttpExchangeScope}.
 */
final class InternalHttpExchangeScope implements Scope {

    /**
     * Key used to store the {@code ExchangeContext} into a {@code HttpServerExchange}.
     */
    private final AttachmentKey<ExchangeContext> contextKey = AttachmentKey.create(ExchangeContext.class);

    /**
     * Thread local storage holding the current assignment of threads to exchange contexts.
     */
    private final ThreadLocal<AtomicReference<ExchangeContext>> contextsByThread =
        new ThreadLocal<AtomicReference<ExchangeContext>>() {
            @Override
            protected AtomicReference<ExchangeContext> initialValue() {
                return new AtomicReference<>();
            }
        };

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return () -> getScopedInstance(key, unscoped);
    }

    <X extends Throwable> void runScoped(final HttpServerExchange exchange,
            final HttpExchangeScope.ScopedAction<X> action) throws X {
        final AtomicReference<ExchangeContext> contextRef = contextsByThread.get();
        final ExchangeContext prevContext = contextRef.getAndSet(getContext(exchange));

        try {
            action.execute();
        } finally {
            if (prevContext == null) {
                contextsByThread.remove();
                contextRef.set(null);
            } else {
                contextRef.set(prevContext);
            }
        }
    }

    private ExchangeContext getContext(final HttpServerExchange exchange) {
        ExchangeContext context = exchange.getAttachment(contextKey);
        if (context == null) {
            synchronized (exchange) {
                context = exchange.getAttachment(contextKey);
                if (context == null) {
                    exchange.putAttachment(contextKey, context = new ExchangeContext(exchange));
                }
            }
        }

        return context;
    }

    private <T> T getScopedInstance(final Key<T> key, final Provider<T> unscoped) {
        final ExchangeContext context = contextsByThread.get().get();
        if (context == null) {
            throw new OutOfScopeException("Cannot access " + key + " outside of a scoping block. "
                    + "Use an instance of HttpExchangeScope to scope code to a specific HttpServerExchange.");
        }

        return context.getScopedInstance(key, unscoped);
    }

    /**
     * Creates scoped instances and holds references to it.
     */
    private static final class ExchangeContext {
        private static final Object NULL_SENTINEL = new Object();

        private final ConcurrentMap<Key<?>, Object> instances = new ConcurrentHashMap<>(8, .75f, 2);

        ExchangeContext(final HttpServerExchange exchange) {

            // store the HttpServerExchange of this context so that it can be injected
            instances.put(Key.get(HttpServerExchange.class), exchange);
        }

        public <T> T getScopedInstance(final Key<T> key, final Provider<T> unscoped) {
            final Object instance = instances.get(key);

            if (instance != null) {
                return deref(instance);
            }

            final T newInstance = unscoped.get();
            if (Scopes.isCircularProxy(newInstance)) {
                return newInstance;
            }

            final Object prevInstance = instances.putIfAbsent(key, ref(newInstance));
            return prevInstance == null ? newInstance : deref(prevInstance);
        }

        private static Object ref(final Object o) {
            return o == null ? NULL_SENTINEL : o;
        }

        @SuppressWarnings("unchecked")
        private static <T> T deref(final Object o) {
            return o == NULL_SENTINEL ? null : (T) o;
        }
    }
}
