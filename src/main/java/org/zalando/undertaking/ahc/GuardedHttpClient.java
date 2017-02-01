package org.zalando.undertaking.ahc;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import io.github.robwin.circuitbreaker.operator.CircuitBreakerOperator;

import io.reactivex.Single;

import io.reactivex.functions.BiPredicate;

/**
 * Helper class to create Http Requests guarded by a retry handler and a circuit breaker.
 *
 * @see  ClientConfig
 */
public class GuardedHttpClient {
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private Function<BoundRequestBuilder, Single<Response>> requestCreator;

    public GuardedHttpClient(final CircuitBreakerRegistry circuitBreakerRegistry,
            final Function<BoundRequestBuilder, Single<Response>> requestCreator) {
        this.circuitBreakerRegistry = requireNonNull(circuitBreakerRegistry);
        this.requestCreator = requireNonNull(requestCreator);
    }

    public <T> Single<T> executeRequest(final BoundRequestBuilder builder, final Function<Response, T> responseHandler,
            final ClientConfig config) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(config.getCircuitBreakerName(),
                buildCircuitBreakerConfig(config));
        //J-
        return requestCreator.apply(builder)
            .map(responseHandler::apply)
            .retry(maxRetriesOr(config.getMaxRetries(), exceptionIsNotOfType(config.getNonRetryableExceptions())))
            .timeout(config.getTimeoutMillis(), TimeUnit.MILLISECONDS, Single.error(new TimeoutException()))
            .lift(CircuitBreakerOperator.of(circuitBreaker));
        //J+
    }

    private CircuitBreakerConfig buildCircuitBreakerConfig(final ClientConfig config) {
        return CircuitBreakerConfig.custom()
                                   .recordFailure(exceptionIsNotOfType(config.getCircuitBreakerIgnoreFailure()))
                                   .build();
    }

    private Predicate<Throwable> exceptionIsNotOfType(final Collection<Class<? extends Throwable>> exceptionTypes) {
        return e -> !exceptionTypes.contains(e.getClass());
    }

    private BiPredicate<Integer, Throwable> maxRetriesOr(final int maxRetries, final Predicate<Throwable> pred) {
        return (tries, ex) -> tries <= maxRetries && pred.test(ex);
    }
}
