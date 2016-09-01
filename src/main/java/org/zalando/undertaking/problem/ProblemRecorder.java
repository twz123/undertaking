package org.zalando.undertaking.problem;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Preconditions.checkState;

import javax.inject.Inject;

import io.undertow.server.HttpServerExchange;

/**
 * Records a problem for a given {@code HttpServerExchange}.
 */
public final class ProblemRecorder extends SimpleProblemSetter<ProblemRecorder> {

    private final HttpServerExchange exchange;

    private boolean recorded;

    @Inject
    ProblemRecorder(final HttpServerExchange exchange) {
        this.exchange = requireNonNull(exchange);
    }

    public static ProblemRecorder forExchange(final HttpServerExchange exchange) {
        return new ProblemRecorder(exchange);
    }

    public void record() {
        checkState(!recorded, "Problem has already been recorded");
        ExchangeProblemStore.putData(exchange, getData());
        ExchangeProblemStore.putError(exchange, getError());
        recorded = true;
    }

    @Override
    protected ProblemRecorder self() {
        return this;
    }
}
