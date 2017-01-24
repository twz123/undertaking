package org.asynchttpclient.extras.rxjava2.single;

import static java.util.Objects.requireNonNull;

import org.asynchttpclient.AsyncHandler;

import io.reactivex.SingleEmitter;

final class AsyncSingleSubscriberBridge<T> extends AbstractSingleSubscriberBridge<T> {

    private final AsyncHandler<? extends T> delegate;

    public AsyncSingleSubscriberBridge(SingleEmitter<T> subscriber, AsyncHandler<? extends T> delegate) {
        super(subscriber);
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected AsyncHandler<? extends T> delegate() {
        return delegate;
    }

}
