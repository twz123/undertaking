package org.asynchttpclient.extras.rxjava2.single;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.extras.rxjava2.AbortedException;
import org.asynchttpclient.extras.rxjava2.UnsubscribedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.SingleEmitter;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;

abstract public class AbstractSingleSubscriberBridge<T> implements AsyncHandler<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSingleSubscriberBridge.class);

    protected final SingleEmitter<T> subscriber;

    private final AtomicBoolean delegateTerminated = new AtomicBoolean();

    protected AbstractSingleSubscriberBridge(SingleEmitter<T> subscriber) {
        this.subscriber = requireNonNull(subscriber);
    }

    @Override
    public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
        return subscriber.isDisposed() ? abort() : delegate().onBodyPartReceived(content);
    }

    @Override
    public State onStatusReceived(HttpResponseStatus status) throws Exception {
        return subscriber.isDisposed() ? abort() : delegate().onStatusReceived(status);
    }

    @Override
    public State onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return subscriber.isDisposed() ? abort() : delegate().onHeadersReceived(headers);
    }

    @Override
    public Void onCompleted() {
        if (delegateTerminated.getAndSet(true)) {
            return null;
        }

        final T result;
        try {
            result = delegate().onCompleted();
        } catch (final Throwable t) {
            emitOnError(t);
            return null;
        }

        if (!subscriber.isDisposed()) {
            if (result == null) {
                subscriber.onError(new AbortedException());
            } else {
                subscriber.onSuccess(result);
            }
        }
        return null;
    }

    @Override
    public void onThrowable(Throwable t) {
        if (delegateTerminated.getAndSet(true)) {
            return;
        }

        Throwable error = t;
        try {
            delegate().onThrowable(t);
        } catch (final Throwable x) {
            error = new CompositeException(Arrays.asList(t, x));
        }

        emitOnError(error);
    }

    protected AsyncHandler.State abort() {
        if (!delegateTerminated.getAndSet(true)) {
            // send a terminal event to the delegate
            // e.g. to trigger cleanup logic
            delegate().onThrowable(new UnsubscribedException());
        }

        return State.ABORT;
    }

    protected abstract AsyncHandler<? extends T> delegate();

    private void emitOnError(Throwable error) {
        Exceptions.throwIfFatal(error);
        if (!subscriber.isDisposed()) {
            subscriber.onError(error);
        } else {
            LOGGER.debug("Not propagating onError after unsubscription: {}", error.getMessage(), error);
        }
    }
}
