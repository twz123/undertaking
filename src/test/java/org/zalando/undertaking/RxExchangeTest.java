package org.zalando.undertaking;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.*;

import java.io.IOException;

import java.lang.reflect.Field;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.*;

import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Single;

import io.undertow.io.Receiver;
import io.undertow.io.Receiver.ErrorCallback;
import io.undertow.io.Receiver.FullStringCallback;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;

import io.undertow.util.SameThreadExecutor;

@RunWith(MockitoJUnitRunner.class)
public class RxExchangeTest {

    @Mock
    private Receiver receiver;

    @Mock
    private HttpHandler handler;

    @Captor
    private ArgumentCaptor<FullStringCallback> fullStringCallback;

    @Captor
    private ArgumentCaptor<ErrorCallback> errorCallback;

    @Captor
    private ArgumentCaptor<Charset> charsetCaptor;

    @Before
    public void initializeTest() {
        doNothing().when(receiver) //
                   .receiveFullString(fullStringCallback.capture(), errorCallback.capture(), charsetCaptor.capture());
    }

    @Test
    public void dispatchesToDispatches() throws Exception {
        doAnswer(invocation -> {
                                    final HttpServerExchange exchange = invocation.getArgument(0);
                                    exchange.dispatch(SameThreadExecutor.INSTANCE, () -> { });
                                    return null;
                                }) //
        .when(handler).handleRequest(any());

        final HttpServerExchange exchange = new HttpServerExchange(null);
        RxExchange.dispatchTo(Single.just(handler)).handleRequest(exchange);
        verify(handler).handleRequest(same(exchange));
    }

    @Test
    public void returnsInternalServerErrorIfSingleErroredOut() throws Exception {
        HttpServerExchange exchange = mock(HttpServerExchange.class,
                withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

        doReturn(exchange).when(exchange).endExchange();

        RxExchange.dispatchTo(Single.error(new RuntimeException("any error"))).handleRequest(exchange);
        assertThat(exchange.getStatusCode(), is(500));

        verify(exchange).endExchange();
    }

    @Test
    public void receiveFullStringForwardsMessage() throws Exception {
        final HttpServerExchange exchange = new HttpServerExchange(mock(ServerConnection.class));
        final Field receiverField = HttpServerExchange.class.getDeclaredField("receiver");
        receiverField.setAccessible(true);
        receiverField.set(exchange, receiver);

        final Single<String> single = RxExchange.receiveFullString(exchange);

        final String message = "foo";
        fullStringCallback.getValue().handle(new HttpServerExchange(null), message);

        assertThat(single.blockingGet(), is(sameInstance(message)));
    }

    @Test
    public void receiveFullStringForwardsError() throws Exception {
        final HttpServerExchange exchange = new HttpServerExchange(mock(ServerConnection.class));
        final Field receiverField = HttpServerExchange.class.getDeclaredField("receiver");
        receiverField.setAccessible(true);
        receiverField.set(exchange, receiver);

        final Single<String> single = RxExchange.receiveFullString(exchange);

        final IOException exception = new IOException("foo");
        errorCallback.getValue().error(new HttpServerExchange(null), exception);

        single.test().assertError(exception);
    }
}
