package org.zalando.undertaking.ahc;

import org.asynchttpclient.AsyncHandler;

import rx.functions.Func0;

/**
 * Supplies {@link AsyncHandler} instances to be used when executing HTTP requests.
 *
 * @param  <T>  type of the result produced by {@code AsyncHandler} instances returned by this supplier
 */
@FunctionalInterface
public interface AsyncHandlerSupplier<T> extends Func0<AsyncHandler<T>> {

    /**
     * Supplies an instance of {@code H} that is ready to be used in for a new HTTP request.
     */
    @Override
    AsyncHandler<T> call();

}
