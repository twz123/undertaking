package org.zalando.undertaking.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;

public class MetricsResponderTest {
    private ByteArrayOutputStream responseStream;
    private HeaderMap responseHeaders;
    private HttpServerExchange exchange;
    private MetricRegistry registry;
    private MetricsResponder underTest;

    @Before
    public void setUp() throws Exception {
        MetricsResponder.Config defaultConfig = new MetricsResponder.Config() { };

        responseStream = new ByteArrayOutputStream();
        responseHeaders = new HeaderMap();
        registry = new MetricRegistry();
        underTest = new MetricsResponder(registry, MetricFilter.ALL, defaultConfig);

        exchange = mock(HttpServerExchange.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

        doReturn(false).when(exchange).isInIoThread();
        doReturn(responseHeaders).when(exchange).getResponseHeaders();
        doReturn(responseStream).when(exchange).getOutputStream();
        doReturn(exchange).when(exchange).dispatch(any(HttpHandler.class));
    }

    @Test
    public void respondsWithMetrics() throws Exception {
        registry.register("thirtySeven", (Gauge<Integer>) () -> 37);

        underTest.handleRequest(exchange);

        assertThat(httpResponse()).describedAs("http response").contains("\"gauges\":{\"thirtySeven\":{\"value\":37}}");
    }

    @Test
    public void dispatchesWhenOnIoThread() throws Exception {
        registry.register("thirtySeven", (Gauge<Integer>) () -> 37);

        doReturn(true).when(exchange).isInIoThread();
        underTest.handleRequest(exchange);

        verify(exchange).dispatch(eq(underTest));
    }

    private String httpResponse() {
        try {
            responseStream.flush();
            return responseStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new AssertionError("Could not get HttpExchange body as string");
        }
    }
}
