package org.zalando.undertaking.problem;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.AttachmentKey;

/**
 * Utility class to attach problem information to {@code HttpServerExchanges}.
 */
final class ExchangeProblemStore {

    private static final AttachmentKey<Map<String, Object>> DATA_KEY = AttachmentKey.create(Map.class);
    private static final AttachmentKey<Throwable> ERROR_KEY = AttachmentKey.create(Throwable.class);
    private static final AttachmentKey<Boolean> SENT_KEY = AttachmentKey.create(Boolean.class);

    static Map<String, ?> getData(final HttpServerExchange exchange) {
        return firstNonNull(exchange.getAttachment(DATA_KEY), ImmutableMap.of());
    }

    static void putData(final HttpServerExchange exchange, final Map<String, Object> data) {
        exchange.putAttachment(DATA_KEY, ImmutableMap.copyOf(data));
    }

    static Optional<Throwable> getError(final HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getAttachment(ERROR_KEY));
    }

    static void putError(final HttpServerExchange exchange, final Optional<Throwable> error) {
        if (error.isPresent()) {
            exchange.putAttachment(ERROR_KEY, error.get());
        } else {
            exchange.removeAttachment(ERROR_KEY);
        }
    }

    static void recordProblemDataSent(final HttpServerExchange exchange) {
        exchange.putAttachment(SENT_KEY, Boolean.TRUE);
    }

    static boolean isProblemDataSent(final HttpServerExchange exchange) {
        return exchange.getAttachment(SENT_KEY) == Boolean.TRUE;
    }

    private ExchangeProblemStore() {
        throw new AssertionError("No instances for you!");
    }

}
