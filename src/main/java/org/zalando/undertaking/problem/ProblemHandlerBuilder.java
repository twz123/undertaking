package org.zalando.undertaking.problem;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import com.google.gson.Gson;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Builds {@code HttpHandlers} that send a {@code application/problem+json} structure back to clients.
 */
public class ProblemHandlerBuilder extends SimpleProblemSetter<ProblemHandlerBuilder> {

    private final Gson gson;

    @Inject
    public ProblemHandlerBuilder(@Internal final Gson gson) {
        this.gson = requireNonNull(gson);
    }

    public HttpHandler build(final int statusCode) {
        return new Handler(gson, statusCode, getData(), getError());
    }

    @Override
    protected ProblemHandlerBuilder self() {
        return this;
    }

    private static final class Handler extends ProblemResponder implements HttpHandler {
        private final Gson gson;
        private final Map<String, Object> problemData;
        private final Optional<Throwable> error;
        private final int statusCode;

        Handler(final Gson gson, final int statusCode, final Map<String, Object> problemData,
                final Optional<Throwable> error) {
            this.gson = requireNonNull(gson);
            this.statusCode = statusCode;
            this.problemData = ImmutableMap.copyOf(problemData);
            this.error = requireNonNull(error);
        }

        @Override
        public String toString() {
            final MoreObjects.ToStringHelper helper =
                MoreObjects.toStringHelper("ProblemHandlerBuilder.Handler") //
                           .addValue(statusCode)                            //
                           .omitNullValues()                                //
                           .add("error", error.orElse(null));

            for (final Map.Entry<String, Object> entry : problemData.entrySet()) {
                helper.add(entry.getKey(), entry.getValue());
            }

            return helper.toString();
        }

        @Override
        public void handleRequest(final HttpServerExchange exchange) {
            exchange.setStatusCode(statusCode);
            sendProblem(exchange, problemData, error);
        }

        @Override
        protected String toJson(final Map<String, ?> problem) {
            return gson.toJson(problem);
        }
    }
}
