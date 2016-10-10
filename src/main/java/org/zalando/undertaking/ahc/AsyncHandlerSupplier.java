package org.zalando.undertaking.ahc;

import org.asynchttpclient.AsyncHandler;

import rx.functions.Func0;

/**
 * Supplies {@link AsyncHandler} instances to be used when executing HTTP requests.
 *
 * <p>Since {@code AsyncHandler} instances are usually stateful, this method should honor that and usually return new
 * instances.</p>
 *
 * @param  <T>  type of the result {@linkplain AsyncHandler#onCompleted() produced} by {@code AsyncHandler} instances
 *              returned by this supplier
 */
@FunctionalInterface
public interface AsyncHandlerSupplier<T> extends Func0<AsyncHandler<T>> {

    /**
     * Supplies an {@code AsyncHandler} instance that is ready to be used to process a HTTP request.
     */
    @Override
    AsyncHandler<T> call();
}
