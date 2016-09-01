package org.zalando.undertaking.inject.rx;

import static java.util.Objects.requireNonNull;

import org.zalando.undertaking.inject.HttpExchangeScope;

import io.undertow.server.HttpServerExchange;

import rx.Observable.Operator;

import rx.Subscriber;

public interface RxHttpExchangeScope extends HttpExchangeScope {

    static <T> Operator<T, T> scoped(final HttpExchangeScope scope, final HttpServerExchange exchange) {
        requireNonNull(exchange);

        return child -> {
            return new Subscriber<T>(child) {
                @Override
                public void onNext(final T value) {
                    scope.runScoped(exchange, () -> child.onNext(value));
                }

                @Override
                public void onError(final Throwable e) {
                    scope.runScoped(exchange, () -> child.onError(e));
                }

                @Override
                public void onCompleted() {
                    scope.runScoped(exchange, child::onCompleted);
                }
            };
        };
    }

    default <T> Operator<T, T> scoped(final HttpServerExchange exchange) {
        return scoped(this, exchange);
    }

}
