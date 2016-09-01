package org.zalando.undertaking.problem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

/**
 * Default implementation for sending problem data to clients encoded as {@code application/problem+json}.
 */
abstract class ProblemResponder {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected void sendProblem(final HttpServerExchange exchange, final Map<String, ?> data,
            final Optional<Throwable> error) {
        final int statusCode = exchange.getStatusCode();
        final String reason = StatusCodes.getReason(statusCode);

        // fill with default values
        final Map<String, Object> problem = new HashMap<>(data.size() + 2);
        problem.put("type", "https://httpstatus.es/" + statusCode);
        problem.put("title", reason);
        problem.putAll(data);

        if (error.isPresent()) {
            final UUID uuid = UUID.randomUUID();
            problem.put("uuid", uuid.toString());

            final Throwable theError = error.get();

            if (statusCode >= StatusCodes.INTERNAL_SERVER_ERROR) {
                log.error("Sending [{} {}]: [{}] [{}]", statusCode, reason, uuid, theError.getMessage(), theError);
            } else {
                log.info("Sending [{} {}]: [{}] [{}]", statusCode, reason, uuid, theError.getMessage(), theError);
            }
        }

        final String json = toJson(problem);
        ExchangeProblemStore.recordProblemDataSent(exchange);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/problem+json; charset=UTF-8");
        exchange.getResponseSender().send(json);
    }

    protected abstract String toJson(final Map<String, ?> problem);
}
