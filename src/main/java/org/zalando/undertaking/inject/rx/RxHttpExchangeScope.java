package org.zalando.undertaking.inject.rx;

import static java.util.Objects.requireNonNull;

import org.zalando.undertaking.inject.HttpExchangeScope;

import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;

import io.reactivex.disposables.Disposable;

import io.undertow.server.HttpServerExchange;

public interface RxHttpExchangeScope extends HttpExchangeScope {

    static <T> ObservableOperator<T, T> scoped(final HttpExchangeScope scope, final HttpServerExchange exchange) {
        requireNonNull(exchange);

        return (Observer<? super T> child) ->
                new Observer<T>() {
                @Override
                public void onSubscribe(final Disposable d) {
                    scope.runScoped(exchange, () -> child.onSubscribe(d));
                }

                @Override
                public void onNext(final T value) {
                    scope.runScoped(exchange, () -> child.onNext(value));
                }

                @Override
                public void onError(final Throwable e) {
                    scope.runScoped(exchange, () -> child.onError(e));
                }

                @Override
                public void onComplete() {
                    scope.runScoped(exchange, child::onComplete);
                }
            };
    }

    default <T> ObservableOperator<T, T> scoped(final HttpServerExchange exchange) {
        return scoped(this, exchange);
    }

    static <T> SingleOperator<T, T> scopedSingle(final HttpExchangeScope scope, final HttpServerExchange exchange) {
        requireNonNull(exchange);

        return (SingleObserver<? super T> child) ->
                new SingleObserver<T>() {
                @Override
                public void onSubscribe(final Disposable d) {
                    scope.runScoped(exchange, () -> child.onSubscribe(d));
                }

                @Override
                public void onError(final Throwable e) {
                    scope.runScoped(exchange, () -> child.onError(e));
                }

                @Override
                public void onSuccess(final T t) {
                    scope.runScoped(exchange, () -> child.onSuccess(t));
                }
            };
    }

    default <T> SingleOperator<T, T> scopedSingle(final HttpServerExchange exchange) {
        return scopedSingle(this, exchange);
    }
}
