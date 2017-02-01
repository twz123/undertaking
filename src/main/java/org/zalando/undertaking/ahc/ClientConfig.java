package org.zalando.undertaking.ahc;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

public class ClientConfig {
    private final Set<Class<? extends Throwable>> nonRetryableExceptions;
    private final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailure;
    private final Long timeoutMillis;
    private final Integer maxRetries;
    private final String circuitBreakerName;

    private ClientConfig(final Set<Class<? extends Throwable>> nonRetryableExceptions,
            final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailure, final Long timeoutMillis,
            final int maxRetries, final String circuitBreakerName) {
        this.nonRetryableExceptions = nonRetryableExceptions;
        this.circuitBreakerIgnoreFailure = circuitBreakerIgnoreFailure;
        this.timeoutMillis = timeoutMillis;
        this.maxRetries = maxRetries;
        this.circuitBreakerName = circuitBreakerName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<Class<? extends Throwable>> getNonRetryableExceptions() {
        return nonRetryableExceptions;
    }

    public Set<Class<? extends Throwable>> getCircuitBreakerIgnoreFailure() {
        return circuitBreakerIgnoreFailure;
    }

    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public static class Builder {
        private Long timeoutMillis = 2000L;
        private Integer maxRetries = 1;
        private String circuitBreakerName = "unnamed";
        private Set<Class<? extends Throwable>> circuitBreakerIgnoreFailures = Collections.emptySet();
        private Set<Class<? extends Throwable>> nonRetryableExceptions = Collections.emptySet();

        private Builder() { }

        public Builder timeOutMs(final Long timeoutMillis) {
            requireNonNull(timeoutMillis);
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder maxRetries(final Integer maxRetries) {
            requireNonNull(maxRetries);
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder circuitBreakerName(final String circuitBreakerName) {
            requireNonNull(circuitBreakerName);
            this.circuitBreakerName = circuitBreakerName;
            return this;
        }

        public Builder circuitBreakerIgnoreFailures(
                final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailures) {
            requireNonNull(circuitBreakerIgnoreFailures);
            this.circuitBreakerIgnoreFailures = circuitBreakerIgnoreFailures;
            return this;
        }

        public Builder nonRetryableExceptions(final Set<Class<? extends Throwable>> nonRetryableExceptions) {
            requireNonNull(nonRetryableExceptions);
            this.nonRetryableExceptions = nonRetryableExceptions;
            return this;
        }

        public ClientConfig build() {
            return new ClientConfig(nonRetryableExceptions, circuitBreakerIgnoreFailures, timeoutMillis, maxRetries,
                    circuitBreakerName);
        }
    }
}
