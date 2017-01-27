package org.zalando.undertaking.metrics;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import com.codahale.metrics.*;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.HttpString;
import io.undertow.util.PathTemplateMatch;

public class PathTemplateBasedMetricsCollectorTest {
    private ExchangeCompletionListener.NextListener noopListener;
    private PathTemplateBasedMetricsCollector underTest;
    private Clock mockClock;
    private MetricRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new MetricRegistry();
        mockClock = mock(Clock.class);

        underTest = new PathTemplateBasedMetricsCollector(new MetricRegistryTimerProvider(registry,
                    () -> new Timer(new SlidingTimeWindowReservoir(1, TimeUnit.MINUTES))), mockClock);

        noopListener = () -> { };
    }

    @Test
    public void ignoresRequestStartTimeErrors() {
        HttpServerExchange exchange = mockExchange("GET", "api/some-url", 200);
        mockStartAndEndTimings(exchange, -1L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        assertThat(registry.getTimers()).describedAs("timers registry").isEmpty();
    }

    @Test
    public void ignoresErrorsWhileSubmittingMetrics() {
        PathTemplateBasedMetricsCollector throwingCollector = new PathTemplateBasedMetricsCollector(s -> {
                    throw new RuntimeException("Some error");
                },
                mockClock);

        HttpServerExchange exchange = mockExchange("GET", "api/some-url", 200);
        mockStartAndEndTimings(exchange, 100, 300L);

        throwingCollector.exchangeEvent(exchange, noopListener);
        assertThat(registry.getTimers()).describedAs("timers registry").isEmpty();
    }

    @Test
    public void recordsPathBasedMetricsGET() {
        HttpServerExchange exchange = mockExchange("GET", "api/some-url", 200);
        mockStartAndEndTimings(exchange, 100L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        Timer timer = getTimer("zmon.response.200.GET.api.some-url");

        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMin()).isEqualTo(200L);
    }

    @Test
    public void recordsPathBasedMetricsPOST() {
        HttpServerExchange exchange = mockExchange("POST", "api/some-url", 200);
        mockStartAndEndTimings(exchange, 100L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        Timer timer = getTimer("zmon.response.200.POST.api.some-url");

        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMin()).isEqualTo(200L);
    }

    @Test
    public void recordsPathBasedMetricsPlaceholders() {
        HttpServerExchange exchange = mockExchange("GET", "api/some-url/{id}/_secret", 200);
        mockStartAndEndTimings(exchange, 100L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        Timer timer = getTimer("zmon.response.200.GET.api.some-url.id._secret");

        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMin()).isEqualTo(200L);
    }

    @Test
    public void recordsPathBasedMetricsRoot() {
        HttpServerExchange exchange = mockExchange("GET", "/", 200);
        mockStartAndEndTimings(exchange, 100L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        Timer timer = getTimer("zmon.response.200.GET.root");

        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMin()).isEqualTo(200L);
    }

    @Test
    public void recordsPathBasedMetricsUnmapped() {
        HttpServerExchange exchange = mockExchange("GET", "/", 200);
        exchange.removeAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        mockStartAndEndTimings(exchange, 100L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        Timer timer = getTimer("zmon.response.200.GET.unmapped");

        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMin()).isEqualTo(200L);
    }

    @Test
    public void recordsStatusCodeBasedMetrics() {
        HttpServerExchange exchange = mockExchange("GET", "api/some-url", 200);
        mockStartAndEndTimings(exchange, 100L, 300L);

        underTest.exchangeEvent(exchange, noopListener);

        Timer timer = getTimer("zmon.response.200.ALL");

        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMin()).isEqualTo(200L);
    }

    @Test
    public void recordsStatusCodeAggregatedBasedMetrics() {
        HttpServerExchange createdExchange = mockExchange("GET", "api/some-url", 201);
        mockStartAndEndTimings(createdExchange, 100L, 300L);
        underTest.exchangeEvent(createdExchange, noopListener);

        HttpServerExchange unavailableExchange = mockExchange("GET", "api/some-url", 503);
        mockStartAndEndTimings(unavailableExchange, 200L, 500L);
        underTest.exchangeEvent(unavailableExchange, noopListener);

        Timer successTimer = getTimer("zmon.response.2XX.ALL");

        assertThat(successTimer.getCount()).isEqualTo(1);
        assertThat(successTimer.getSnapshot().getMin()).isEqualTo(200L);

        Timer serverErrorTimer = getTimer("zmon.response.5XX.ALL");

        assertThat(serverErrorTimer.getCount()).isEqualTo(1);
        assertThat(serverErrorTimer.getSnapshot().getMin()).isEqualTo(300L);
    }

    private Timer getTimer(final String name) {
        Timer timer = registry.getTimers(MetricFilter.ALL).get(name);

        assertThat(timer).describedAs("timer " + name).isNotNull();

        return timer;
    }

    private void mockStartAndEndTimings(final HttpServerExchange exchange, final long start, final long end) {
        when(exchange.getRequestStartTime()).thenReturn(start);
        when(mockClock.getTick()).thenReturn(end);
    }

    private HttpServerExchange mockExchange(final String requestMethod, final String requestPath,
            final int statusCode) {
        HttpServerExchange exchange = mock(HttpServerExchange.class,
                withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        exchange.setStatusCode(statusCode);
        exchange.setRequestMethod(HttpString.tryFromString(requestMethod));
        exchange.putAttachment(PathTemplateMatch.ATTACHMENT_KEY,
            new PathTemplateMatch(requestPath, Collections.emptyMap()));
        return exchange;
    }

}
