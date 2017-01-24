package org.asynchttpclient.extras.rxjava2.single;

import static java.util.Objects.requireNonNull;

import org.asynchttpclient.handler.ProgressAsyncHandler;

import io.reactivex.SingleEmitter;

final class ProgressAsyncSingleSubscriberBridge<T> extends AbstractProgressSingleSubscriberBridge<T> {

    private final ProgressAsyncHandler<? extends T> delegate;

    public ProgressAsyncSingleSubscriberBridge(SingleEmitter<T> subscriber, ProgressAsyncHandler<? extends T> delegate) {
        super(subscriber);
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected ProgressAsyncHandler<? extends T> delegate() {
        return delegate;
    }

}
