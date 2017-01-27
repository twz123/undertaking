package org.zalando.undertaking.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.util.Map;

import org.assertj.core.api.MapAssert;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import io.undertow.server.HttpServerExchange;

public class ProblemRecorderTest {
    private HttpServerExchange exchange;
    private ProblemRecorder underTest;

    @Before
    public void setUp() throws Exception {
        exchange = mock(HttpServerExchange.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

        underTest = ProblemRecorder.forExchange(exchange);
        doReturn(exchange).when(exchange).endExchange();
    }

    @Test
    public void storesExceptionOnExchange() {
        RuntimeException exception = new RuntimeException("u ded");

        underTest.setError(exception);
        underTest.record();

        assertThat(ExchangeProblemStore.getError(exchange)).describedAs("Exchange Problem Store error") //
                                                           .isPresent()                                 //
                                                           .containsSame(exception);

    }

    @Test
    public void storesDetailOnExchange() {
        underTest.setDetail("some detail string");
        underTest.record();

        asssertExchangeProblemStoreData().containsEntry("detail", "some detail string");
    }

    @Test
    public void storesTitleOnExchange() {
        underTest.setTitle("some title string");
        underTest.record();

        asssertExchangeProblemStoreData().containsEntry("title", "some title string");
    }

    @Test
    public void storesInstanceOnExchange() {
        underTest.setInstance("some instance");
        underTest.record();

        asssertExchangeProblemStoreData().containsEntry("instance", "some instance");
    }

    @Test
    public void storesTypeOnExchange() {
        underTest.setType("cool");
        underTest.record();

        asssertExchangeProblemStoreData().containsEntry("type", "cool");
    }

    @Test
    public void storesNumericParametersOnExchange() {
        underTest.setParameter("answer", 42);
        underTest.record();

        asssertExchangeProblemStoreData().containsEntry("answer", 42);
    }

    @Test
    public void storesStringParametersOnExchange() {
        underTest.setParameter("answer", "dying");
        underTest.record();

        asssertExchangeProblemStoreData().containsEntry("answer", "dying");
    }

    @Test
    public void throwsIfAlreadyRecorded() {
        underTest.setDetail("some detail string");
        underTest.record();

        assertThatThrownBy(() -> underTest.record()).isInstanceOf(IllegalStateException.class) //
                                .hasMessageContaining("Problem has already been recorded");    //
    }

    @SuppressWarnings("unchecked")
    private MapAssert<String, Object> asssertExchangeProblemStoreData() {
        return assertThat((Map<String, Object>) ExchangeProblemStore.getData(exchange)).describedAs(
                "Exchange Problem Store Data");
    }
}
