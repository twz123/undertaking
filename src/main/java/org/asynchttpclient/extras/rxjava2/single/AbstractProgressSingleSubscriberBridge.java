package org.asynchttpclient.extras.rxjava2.single;

import org.asynchttpclient.handler.ProgressAsyncHandler;

import io.reactivex.SingleEmitter;

abstract public class AbstractProgressSingleSubscriberBridge<T> extends AbstractSingleSubscriberBridge<T> implements ProgressAsyncHandler<Void> {
    protected AbstractProgressSingleSubscriberBridge(SingleEmitter<T> subscriber) {
        super(subscriber);
    }

    @Override
    public State onHeadersWritten() {
        return subscriber.isDisposed() ? abort() : delegate().onHeadersWritten();
    }

    @Override
    public State onContentWritten() {
        return subscriber.isDisposed() ? abort() : delegate().onContentWritten();
    }

    @Override
    public State onContentWriteProgress(long amount, long current, long total) {
        return subscriber.isDisposed() ? abort() : delegate().onContentWriteProgress(amount, current, total);
    }

    @Override
    protected abstract ProgressAsyncHandler<? extends T> delegate();

}
