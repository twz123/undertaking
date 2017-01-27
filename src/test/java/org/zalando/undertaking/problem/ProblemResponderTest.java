package org.zalando.undertaking.problem;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;

import com.google.gson.Gson;

import io.undertow.io.Sender;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class ProblemResponderTest {
    private HeaderMap responseHeaders;
    private String sent;
    private HttpServerExchange exchange;
    private ProblemDefaultResponseHandler underTest;

    @Before
    public void setUp() throws Exception {
        responseHeaders = new HeaderMap();
        sent = null;

        underTest = new ProblemDefaultResponseHandler(new Gson());

        exchange = mock(HttpServerExchange.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

        doReturn(false).when(exchange).isInIoThread();
        doReturn(responseHeaders).when(exchange).getResponseHeaders();
        doReturn(exchange).when(exchange).dispatch(any(HttpHandler.class));

        Sender sender = mock(Sender.class);
        doAnswer(invocation -> sent = invocation.getArgument(0)).when(sender).send(anyString());
        doReturn(sender).when(exchange).getResponseSender();
    }

    @Test
    public void handleRequestAddsResponseListenerAndDelegates() throws Exception {
        HttpHandler handler = mock(HttpHandler.class);

        underTest.setNext(handler);
        underTest.handleRequest(exchange);

        verify(exchange).addDefaultResponseListener(eq(underTest));
        verify(handler).handleRequest(eq(exchange));
    }

    @Test
    public void doesNotHandleDefaultResponseWhenAlreadyStarted() throws Exception {
        doReturn(true).when(exchange).isResponseStarted();
        ExchangeProblemStore.putError(exchange, Optional.of(new RuntimeException("u ded")));

        assertThat(underTest.handleDefaultResponse(exchange)).isFalse();
    }

    @Test
    public void doesNotHandleTwice() throws Exception {
        exchange.setStatusCode(500);
        ExchangeProblemStore.putError(exchange, Optional.of(new RuntimeException("u ded")));
        assertThat(underTest.handleDefaultResponse(exchange)).isTrue();

        doReturn(true).when(exchange).isResponseStarted();

        assertThat(underTest.handleDefaultResponse(exchange)).isFalse();
        assertThat(ExchangeProblemStore.isProblemDataSent(exchange)).isTrue();
    }

    @Test
    public void doesNotHandleDefaultResponseWhenNoErrorOccurred() throws Exception {
        exchange.setStatusCode(200);

        assertThat(underTest.handleDefaultResponse(exchange)).isFalse();
    }

    @Test
    public void sendsErrorJson() throws Exception {
        exchange.setStatusCode(500);
        ExchangeProblemStore.putError(exchange, Optional.of(new RuntimeException("u ded")));

        assertThat(underTest.handleDefaultResponse(exchange)).isTrue();
        assertThat(responseHeaders.getFirst(Headers.CONTENT_TYPE)).isEqualTo("application/problem+json; charset=UTF-8");
        assertThat(sent).as("HTTP Response")                   //
                        .contains("https://httpstatus.es/500") //
                        .contains("Internal Server Error");
    }

    @Test
    public void sendsErrorJsonAdditionalData() throws Exception {
        exchange.setStatusCode(404);
        ExchangeProblemStore.putError(exchange, Optional.of(new RuntimeException("u ded")));
        ExchangeProblemStore.putData(exchange, ImmutableMap.of("additional-data", "is-present"));

        assertThat(underTest.handleDefaultResponse(exchange)).isTrue();
        assertThat(responseHeaders.getFirst(Headers.CONTENT_TYPE)).isEqualTo("application/problem+json; charset=UTF-8");
        assertThat(sent).as("HTTP Response")                   //
                        .contains("https://httpstatus.es/404") //
                        .contains("Not Found")                 //
                        .contains("additional-data")           //
                        .contains("is-present");
    }
}
