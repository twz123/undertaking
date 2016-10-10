package org.zalando.undertaking.ahc;

import org.asynchttpclient.AsyncCompletionHandlerBase;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import rx.Single;

public interface RxHttpClient {

    static RxHttpClient using(final AsyncHttpClient ahc) {
        return new SimpleRxHttpClient(ahc);
    }

    <T> Single<T> prepareRequest(final Request request, AsyncHandlerSupplier<? extends T> handlerSupplier);

    default Single<Response> prepareRequest(final Request request) {
        return prepareRequest(request, AsyncCompletionHandlerBase::new);
    }

    default Single<Response> prepareRequest(final RequestBuilder requestBuilder) {
        return prepareRequest(requestBuilder.build());
    }

    default <T> Single<T> prepareRequest(final RequestBuilder requestBuilder,
            final AsyncHandlerSupplier<? extends T> handlerSupplier) {
        return prepareRequest(requestBuilder.build(), handlerSupplier);
    }
}
