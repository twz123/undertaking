package org.zalando.undertaking.handlers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class MoreHandlersTest {

    private HttpServerExchange exchange;

    @Before
    public void setUp() throws Exception {
        exchange = mock(HttpServerExchange.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        doReturn(exchange).when(exchange).endExchange();
    }

    @Test
    public void endExchangeHandler() throws Exception {
        HttpHandler underTest = MoreHandlers.endExchange();

        underTest.handleRequest(exchange);

        verify(exchange).endExchange();
        verifyNoMoreInteractions(exchange);
    }

    @Test
    public void endExchangeAfterHandler() throws Exception {
        HttpHandler intermediate = mock(HttpHandler.class);
        HttpHandler underTest = MoreHandlers.endExchangeAfter(intermediate);

        underTest.handleRequest(exchange);

        verify(intermediate).handleRequest(eq(exchange));
        verify(exchange).endExchange();
        verifyNoMoreInteractions(exchange);
        verifyNoMoreInteractions(intermediate);
    }
}
