package org.zalando.undertaking.ahc;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.*;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Single;

@RunWith(MockitoJUnitRunner.class)
public class SimpleRxHttpClientTest {

    @Mock
    private AsyncHttpClient ahc;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    @Captor
    private ArgumentCaptor<? extends AsyncHandler<?>> handlerCaptor;

    @InjectMocks
    private SimpleRxHttpClient underTest;

    @Mock
    private Response mockResponse;

    @Mock
    private HttpResponseStatus status;

    @Before
    public void initializeTest() {
        when(ahc.executeRequest(requestCaptor.capture(), handlerCaptor.capture())).then(invocation -> {
            final AsyncHandler<Object> argument = invocation.getArgument(1);
            argument.onStatusReceived(status);
            argument.onCompleted();
            return new CompletedFuture<>(new ReturnMockResponseAsyncHandler());
        });
    }

    @Test
    public void toStringContainsUsefulInfo() {
        assertThat(underTest.toString(), stringContainsInOrder(Arrays.asList("SimpleRxHttpClient", "ahc")));
    }

    @Test
    public void executesRequest() {
        final Request request = Dsl.get("http://example.com").build();

        final Single<Response> requestSingle = underTest.prepareRequest(request, ReturnMockResponseAsyncHandler::new);
        verifyZeroInteractions(ahc);

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(), contains(request));
        assertThat(handlerCaptor.getAllValues(), hasSize(1));

        final AsyncHandler<?> lastHandler = handlerCaptor.getValue();

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(), contains(request, request));
        assertThat(handlerCaptor.getAllValues(), contains(is(lastHandler), not(lastHandler)));
    }

    @Test
    public void executesRequestFromBuilder() {
        final RequestBuilder requestBuilder = Dsl.get("http://example.com");
        final Request request = requestBuilder.build();

        final Single<Response> requestSingle = underTest.prepareRequest(requestBuilder,
                ReturnMockResponseAsyncHandler::new);
        verifyZeroInteractions(ahc);

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(), contains(samePropertyValuesAs(request)));
        assertThat(handlerCaptor.getAllValues(), hasSize(1));

        final AsyncHandler<?> lastHandler = handlerCaptor.getValue();

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(),
            contains(samePropertyValuesAs(request), samePropertyValuesAs(request)));
        assertThat(handlerCaptor.getAllValues(), contains(is(lastHandler), not(lastHandler)));
    }

    @Test
    public void executesRequestWithDefaultHandler() {
        final Request request = Dsl.get("http://example.com").build();

        final Single<Response> requestSingle = underTest.prepareRequest(request);
        verifyZeroInteractions(ahc);

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(), contains(request));
        assertThat(handlerCaptor.getAllValues(), hasSize(1));

        final AsyncHandler<?> lastHandler = handlerCaptor.getValue();

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(), contains(request, request));
        assertThat(handlerCaptor.getAllValues(), contains(is(lastHandler), not(lastHandler)));
    }

    @Test
    public void executesRequestFromBuilderWithDefaultHandler() {
        final RequestBuilder requestBuilder = Dsl.get("http://example.com");
        final Request request = requestBuilder.build();

        final Single<Response> requestSingle = underTest.prepareRequest(requestBuilder);
        verifyZeroInteractions(ahc);

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(), contains(samePropertyValuesAs(request)));
        assertThat(handlerCaptor.getAllValues(), hasSize(1));

        final AsyncHandler<?> lastHandler = handlerCaptor.getValue();

        requestSingle.blockingGet();

        assertThat(requestCaptor.getAllValues(),
            contains(samePropertyValuesAs(request), samePropertyValuesAs(request)));
        assertThat(handlerCaptor.getAllValues(), contains(is(lastHandler), not(lastHandler)));
    }

    @Test
    public void isDefaultImplementationOfRxHttpClient() {
        assertThat(RxHttpClient.using(ahc), instanceOf(SimpleRxHttpClient.class));
    }

    private static final class CompletedFuture<V> implements ListenableFuture<V> {

        private final V value;

        CompletedFuture(final V value) {
            this.value = value;
        }

        @Override
        public void done() {
            // no-op
        }

        @Override
        public void abort(final Throwable t) {
            // no-op
        }

        @Override
        public void touch() {
            // no-op
        }

        @Override
        public CompletableFuture<V> toCompletableFuture() {
            return CompletableFuture.completedFuture(value);
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() {
            return value;
        }

        @Override
        public V get(final long timeout, final TimeUnit unit) {
            return value;
        }

        @Override
        public ListenableFuture<V> addListener(final Runnable listener, final Executor exec) {
            exec.execute(listener);
            return this;
        }
    }

    private class ReturnMockResponseAsyncHandler implements AsyncHandler<Response> {
        @Override
        public void onThrowable(final Throwable throwable) { }

        @Override
        public State onBodyPartReceived(final HttpResponseBodyPart httpResponseBodyPart) throws Exception {
            return State.CONTINUE;
        }

        @Override
        public State onStatusReceived(final HttpResponseStatus httpResponseStatus) throws Exception {
            return State.CONTINUE;
        }

        @Override
        public State onHeadersReceived(final HttpResponseHeaders httpResponseHeaders) throws Exception {
            return State.CONTINUE;
        }

        @Override
        public Response onCompleted() throws Exception {
            return mockResponse;
        }
    }
}
