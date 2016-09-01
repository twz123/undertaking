package org.zalando.undertaking.problem;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.gson.Gson;

import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;

import io.undertow.util.StatusCodes;

/**
 * Adds a {@link DefaultResponseListener} to an {@code HttpServerExchange} that handles problem data recorded via
 * {@link ProblemRecorder} by sending a {@code application/problem+json} response.
 *
 * <p>Subclasses may modify the lookup of problem data by overriding {@link #getData(HttpServerExchange)} and
 * {@link #getError(HttpServerExchange)}.</p>
 */
public class ProblemDefaultResponseHandler extends ProblemResponder implements HttpHandler, DefaultResponseListener {

    private final Gson gson;

    private volatile HttpHandler next = ResponseCodeHandler.HANDLE_404;

    @Inject
    public ProblemDefaultResponseHandler(@Internal final Gson gson) {
        this.gson = requireNonNull(gson);
    }

    public ProblemDefaultResponseHandler setNext(final HttpHandler next) {
        this.next = requireNonNull(next);
        return this;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.addDefaultResponseListener(this);
        next.handleRequest(exchange);
    }

    @Override
    public boolean handleDefaultResponse(final HttpServerExchange exchange) {
        final int statusCode = exchange.getStatusCode();
        final Optional<Throwable> error = getError(exchange);

        if (exchange.isResponseStarted()) {
            if (error.isPresent() && !ExchangeProblemStore.isProblemDataSent(exchange)) {
                final Throwable theError = error.get();
                log.warn("An error has been recorded, but the response has already been started: [{}] [{}]", statusCode,
                    theError.getMessage(), theError);
            }

            return false;
        }

        if (statusCode >= StatusCodes.BAD_REQUEST) {
            sendProblem(exchange, getData(exchange), error);
            return true;
        }

        return false;
    }

    protected Map<String, ?> getData(final HttpServerExchange exchange) {
        return ExchangeProblemStore.getData(exchange);
    }

    protected Optional<Throwable> getError(final HttpServerExchange exchange) {
        return ExchangeProblemStore.getError(exchange);
    }

    @Override
    protected String toJson(final Map<String, ?> problem) {
        return gson.toJson(problem);
    }
}
