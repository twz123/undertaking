package org.zalando.undertaking.ahc;

import org.asynchttpclient.AsyncCompletionHandlerBase;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import io.reactivex.Single;

/**
 * An abstraction of {@link AsyncHttpClient} that exposes HTTP requests and their outcome as RxJava Singles.
 */
public interface RxHttpClient {

    /**
     * Creates an {@code RxHttpClient} instance that uses the given {@code AsyncHttpClient} instance to actually execute
     * requests.
     *
     * @param   ahc  the {@code AsyncHttpClient} instance to use
     *
     * @return  an {@code RxHttpClient} using {@code ahc} under the hood
     */
    static RxHttpClient using(final AsyncHttpClient ahc) {
        return new SimpleRxHttpClient(ahc);
    }

    /**
     * Prepares a HTTP request using the specified {@code handlerSupplier}.
     *
     * @param   request          the request to be executed when the returned {@code Single} is subscribed to
     * @param   handlerSupplier  used to obtain {@code AsyncHandler} instances for HTTP request processing
     *
     * @return  a {@code Single} that executes {@code request} and emits the result produced by the {@code AsyncHandler}
     *          obtained from {@code handlerSupplier}
     *
     * @throws  NullPointerException  if at least one of the arguments is {@code null}
     */
    <T> Single<T> prepareRequest(final Request request, AsyncHandlerSupplier<? extends T> handlerSupplier);

    /**
     * Prepares a HTTP request using {@link AsyncCompletionHandlerBase} handler instances for request processing.
     *
     * @param   request  the request to be executed when the returned {@code Single} is subscribed to
     *
     * @return  a {@code Single} that executes {@code request} and emits the {@link Response}
     *
     * @throws  NullPointerException  if {@code request} is {@code null}
     */
    default Single<Response> prepareRequest(final Request request) {
        return prepareRequest(request, AsyncCompletionHandlerBase::new);
    }

    /**
     * Convenience method to prepare a HTTP request. Builds the {@code Request} using the given {@code requestBuilder}
     * and delegates it to {@link #prepareRequest(Request)}.
     *
     * @param   requestBuilder  the request builder from which to obtain the {@code Request} instance.
     *
     * @return  a {@code Single} that executes the {@code Request} built by {@code requestBuilder} and emits the
     *          {@link Response}
     *
     * @throws  NullPointerException  if {@code requestBuilder} is {@code null}
     */
    default Single<Response> prepareRequest(final RequestBuilder requestBuilder) {
        return prepareRequest(requestBuilder.build());
    }

    /**
     * Convenience method to prepare a HTTP request. Builds the {@code Request} using the given {@code requestBuilder}
     * and then delegates to {@link #prepareRequest(Request, AsyncHandlerSupplier)}.
     *
     * @param   requestBuilder   the request builder from which to obtain the {@code Request} instance.
     * @param   handlerSupplier  used to obtain {@code AsyncHandler} instances for HTTP request processing
     *
     * @return  a {@code Single} that executes the {@code Request} built by {@code requestBuilder} and emits the
     *          {@link Response}*
     *
     * @throws  NullPointerException  if {@code requestBuilder} is {@code null}
     */
    default <T> Single<T> prepareRequest(final RequestBuilder requestBuilder,
            final AsyncHandlerSupplier<? extends T> handlerSupplier) {
        return prepareRequest(requestBuilder.build(), handlerSupplier);
    }
}
