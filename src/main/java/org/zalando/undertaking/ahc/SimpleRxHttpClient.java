package org.zalando.undertaking.ahc;

import static java.util.Objects.requireNonNull;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;

import org.asynchttpclient.extras.rxjava.single.AsyncHttpSingle;

import com.google.common.base.MoreObjects;

import rx.Single;

/**
 * Straight-forward implementation of {@code RxHttpClient} that uses an {@code AsyncHttpClient} instance to actually
 * execute HTTP requests.
 */
final class SimpleRxHttpClient implements RxHttpClient {

    private final AsyncHttpClient ahc;

    public SimpleRxHttpClient(final AsyncHttpClient ahc) {
        this.ahc = requireNonNull(ahc);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(ahc).toString();
    }

    @Override
    public <T> Single<T> prepareRequest(final Request request,
            final AsyncHandlerSupplier<? extends T> handlerSupplier) {

        requireNonNull(request);
        requireNonNull(handlerSupplier);

        return AsyncHttpSingle.create(handler -> ahc.executeRequest(request, handler), handlerSupplier);
    }
}
