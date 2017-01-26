package org.zalando.undertaking.metrics;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class PathTemplateBasedMetricsHandlerTest {

    private PathTemplateBasedMetricsCollector collector;
    private PathTemplateBasedMetricsHandler underTest;
    private HttpServerExchange exchange;

    @Before
    public void setUp() throws Exception {
        collector = mock(PathTemplateBasedMetricsCollector.class);
        underTest = new PathTemplateBasedMetricsHandler(collector);
        exchange = mock(HttpServerExchange.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void attachesExchangeCompletionlistener() throws Exception {
        underTest.setNext((ex) -> { });
        underTest.handleRequest(exchange);

        verify(exchange).addExchangeCompleteListener(eq(collector));
    }

    @Test
    public void delegatesToNextHandler() throws Exception {
        HttpHandler handler = mock(HttpHandler.class);
        underTest.setNext(handler);
        underTest.handleRequest(exchange);

        verify(handler).handleRequest(eq(exchange));
    }
}
