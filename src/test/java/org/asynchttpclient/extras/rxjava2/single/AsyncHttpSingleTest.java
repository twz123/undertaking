package org.asynchttpclient.extras.rxjava2.single;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.asynchttpclient.AsyncCompletionHandlerBase;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;

import org.asynchttpclient.extras.rxjava2.AbortedException;
import org.asynchttpclient.extras.rxjava2.UnsubscribedException;

import org.asynchttpclient.handler.ProgressAsyncHandler;

import org.junit.Test;

import org.mockito.InOrder;

import io.reactivex.Single;

import io.reactivex.exceptions.CompositeException;

import io.reactivex.observers.TestObserver;

public class AsyncHttpSingleTest {

    @Test(expected = NullPointerException.class)
    public void testFailsOnNullRequest() {
        AsyncHttpSingle.create((BoundRequestBuilder) null);
    }

    @Test(expected = NullPointerException.class)
    public void testFailsOnNullHandlerSupplier() {
        AsyncHttpSingle.create(mock(BoundRequestBuilder.class), null);
    }

    @Test
    public void testSuccessfulCompletion() throws Exception {

        @SuppressWarnings("unchecked")
        final AsyncHandler<Object> handler = mock(AsyncHandler.class);
        when(handler.onCompleted()).thenReturn(handler);

        final Single<Object> underTest = AsyncHttpSingle.create(bridge -> {
                    try {
                        assertThat(bridge, is(not(instanceOf(ProgressAsyncHandler.class))));

                        bridge.onStatusReceived(null);
                        verify(handler).onStatusReceived(null);

                        bridge.onHeadersReceived(null);
                        verify(handler).onHeadersReceived(null);

                        bridge.onBodyPartReceived(null);
                        verify(handler).onBodyPartReceived(null);

                        bridge.onCompleted();
                        verify(handler).onCompleted();
                    } catch (final Throwable t) {
                        bridge.onThrowable(t);
                    }

                    return mock(Future.class);
                },
                () -> handler);

        underTest.test().assertResult(handler);

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testSuccessfulCompletionWithProgress() throws Exception {

        @SuppressWarnings("unchecked")
        final ProgressAsyncHandler<Object> handler = mock(ProgressAsyncHandler.class);
        when(handler.onCompleted()).thenReturn(handler);

        final InOrder inOrder = inOrder(handler);

        final Single<Object> underTest = AsyncHttpSingle.create(bridge -> {
                    try {
                        assertThat(bridge, is(instanceOf(ProgressAsyncHandler.class)));

                        final ProgressAsyncHandler<?> progressBridge = (ProgressAsyncHandler<?>) bridge;

                        progressBridge.onHeadersWritten();
                        inOrder.verify(handler).onHeadersWritten();

                        progressBridge.onContentWriteProgress(60, 40, 100);
                        inOrder.verify(handler).onContentWriteProgress(60, 40, 100);

                        progressBridge.onContentWritten();
                        inOrder.verify(handler).onContentWritten();

                        progressBridge.onStatusReceived(null);
                        inOrder.verify(handler).onStatusReceived(null);

                        progressBridge.onHeadersReceived(null);
                        inOrder.verify(handler).onHeadersReceived(null);

                        progressBridge.onBodyPartReceived(null);
                        inOrder.verify(handler).onBodyPartReceived(null);

                        progressBridge.onCompleted();
                        inOrder.verify(handler).onCompleted();
                    } catch (final Throwable t) {
                        bridge.onThrowable(t);
                    }

                    return mock(Future.class);
                },
                () -> handler);

        underTest.test().assertValue(handler);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testNewRequestForEachSubscription() throws Exception {
        final BoundRequestBuilder builder = mock(BoundRequestBuilder.class);

        final Single<Response> underTest = AsyncHttpSingle.create(builder);
        underTest.subscribe((o) -> { });
        underTest.subscribe((o) -> { });

        verify(builder, times(2)).execute(any());
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void testErrorPropagation() throws Exception {
        final RuntimeException expectedException = new RuntimeException("expected");
        @SuppressWarnings("unchecked")
        final AsyncHandler<Object> handler = mock(AsyncHandler.class);
        when(handler.onCompleted()).thenReturn(handler);

        final InOrder inOrder = inOrder(handler);

        final Single<?> underTest = AsyncHttpSingle.create(bridge -> {
                    try {
                        bridge.onStatusReceived(null);
                        inOrder.verify(handler).onStatusReceived(null);

                        bridge.onHeadersReceived(null);
                        inOrder.verify(handler).onHeadersReceived(null);

                        bridge.onBodyPartReceived(null);
                        inOrder.verify(handler).onBodyPartReceived(null);

                        bridge.onThrowable(expectedException);
                        inOrder.verify(handler).onThrowable(expectedException);

                        // test that no further events are invoked after terminal events
                        bridge.onCompleted();
                        inOrder.verify(handler, never()).onCompleted();
                    } catch (final Throwable t) {
                        bridge.onThrowable(t);
                    }

                    return mock(Future.class);
                },
                () -> handler);

        underTest.test().assertError(expectedException);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testErrorInOnCompletedPropagation() throws Exception {

        final RuntimeException expectedException = new RuntimeException("expected");
        @SuppressWarnings("unchecked")
        final AsyncHandler<Object> handler = mock(AsyncHandler.class);
        when(handler.onCompleted()).thenThrow(expectedException);

        final Single<?> underTest = AsyncHttpSingle.create(bridge -> {
                    try {
                        bridge.onCompleted();
                        return mock(Future.class);
                    } catch (final Throwable t) {
                        throw new AssertionError(t);
                    }
                },
                () -> handler);

        underTest.test().assertError(expectedException);

        verify(handler).onCompleted();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testErrorInOnThrowablePropagation() throws Exception {

        final RuntimeException processingException = new RuntimeException("processing");
        final RuntimeException thrownException = new RuntimeException("thrown");
        @SuppressWarnings("unchecked")
        final AsyncHandler<Object> handler = mock(AsyncHandler.class);
        doThrow(thrownException).when(handler).onThrowable(processingException);

        final Single<?> underTest = AsyncHttpSingle.create(bridge -> {
                    try {
                        bridge.onThrowable(processingException);
                        return mock(Future.class);
                    } catch (final Throwable t) {
                        throw new AssertionError(t);
                    }
                },
                () -> handler);

        TestObserver<?> testObserver = underTest.test();
        testObserver.assertError(CompositeException.class);

        List<Throwable> errors = testObserver.errors();

        final CompositeException error = (CompositeException) errors.get(0);
        assertEquals(error.getExceptions(), Arrays.asList(processingException, thrownException));

        verify(handler).onThrowable(processingException);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testAbort() throws Exception {
        try(AsyncHttpClient client = asyncHttpClient()) {
            final Single<Object> underTest = AsyncHttpSingle.create(client.prepareGet("http://github.com"),
                    () ->
                        new AsyncCompletionHandlerBase() {
                        @Override
                        public State onStatusReceived(final HttpResponseStatus status) {
                            return State.ABORT;
                        }
                    });

            underTest.test().awaitDone(10, TimeUnit.SECONDS).assertError(AbortedException.class);
        }
    }

    @Test
    public void testUnsubscribe() throws Exception {
        @SuppressWarnings("unchecked")
        final AsyncHandler<Object> handler = mock(AsyncHandler.class);
        final Future<?> future = mock(Future.class);
        final AtomicReference<AsyncHandler<?>> bridgeRef = new AtomicReference<>();

        final Single<?> underTest = AsyncHttpSingle.create(bridge -> {
                    bridgeRef.set(bridge);
                    return future;
                },
                () -> handler);

        underTest.subscribe().dispose();
        verify(future).cancel(true);
        verifyZeroInteractions(handler);

        assertThat(bridgeRef.get().onStatusReceived(null), is(AsyncHandler.State.ABORT));
        verify(handler).onThrowable(isA(UnsubscribedException.class));
        verifyNoMoreInteractions(handler);
    }
}
