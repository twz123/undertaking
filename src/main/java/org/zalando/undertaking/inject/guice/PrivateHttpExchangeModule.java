package org.zalando.undertaking.inject.guice;

import java.util.function.Consumer;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.inject.HttpExchangeScoped;
import org.zalando.undertaking.inject.HttpExchangeScopedExecutor;
import org.zalando.undertaking.inject.Request;

import com.google.inject.Exposed;
import com.google.inject.PrivateBinder;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;

// here go bindings that need the HttpServerExchange instance
final class PrivateHttpExchangeModule extends PrivateModule {

    private final Iterable<? extends Consumer<? super PrivateBinder>> internalModules;

    PrivateHttpExchangeModule(final Iterable<? extends Consumer<? super PrivateBinder>> internalModules) {
        this.internalModules = internalModules;
    }

    @Override
    protected void configure() {
        for (final Consumer<? super PrivateBinder> consumer : internalModules) {
            consumer.accept(binder());
        }
    }

    @Provides
    @HttpExchangeScoped
    HttpServerExchange provideExchange() {
        throw new IllegalStateException(                                       //
            "No HttpServerExchange instance bound. This typically denotes an " //
                + "internal error of the HttpExchangeScope implementation.");
    }

    @Provides
    @HttpExchangeScoped
    HttpExchangeScopedExecutor provideHttpExchangeScopedExecutor(final HttpExchangeScope scope,
            final HttpServerExchange exchange) {
        return scope.createExecutorFor(exchange);
    }

    @Exposed
    @Provides
    @Request
    @HttpExchangeScoped
    HeaderMap provideRequestHeaders(final HttpServerExchange exchange) {
        return exchange.getRequestHeaders();
    }

}
