package org.zalando.undertaking.ahc;

import java.util.Collections;
import java.util.Set;

public class ClientConfig {
    private final Set<Class<? extends Throwable>> nonRetryableExceptions;
    private final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailure;
    private final Integer timeoutMs;
    private final Integer maxRetries;
    private final String circuitBreakerName;

    private ClientConfig(final Set<Class<? extends Throwable>> nonRetryableExceptions,
            final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailure, final int timeoutMs,
            final int maxRetries, final String circuitBreakerName) {
        this.nonRetryableExceptions = nonRetryableExceptions;
        this.circuitBreakerIgnoreFailure = circuitBreakerIgnoreFailure;
        this.timeoutMs = timeoutMs;
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

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public static class Builder {
        private Integer timeOutMs = 2000;
        private Integer maxRetries = 1;
        private String circuitBreakerName = "unnamed";
        private Set<Class<? extends Throwable>> circuitBreakerIgnoreFailures = Collections.emptySet();
        private Set<Class<? extends Throwable>> nonRetryableExceptions = Collections.emptySet();

        private Builder() { }

        public Builder timeOutMs(final Integer timeOutMs) {
            this.timeOutMs = timeOutMs;
            return this;
        }

        public Builder maxRetries(final Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder circuitBreakerName(final String circuitBreakerName) {
            this.circuitBreakerName = circuitBreakerName;
            return this;
        }

        public Builder circuitBreakerIgnoreFailures(
                final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailures) {
            this.circuitBreakerIgnoreFailures = circuitBreakerIgnoreFailures;
            return this;
        }

        public Builder nonRetryableExceptions(final Set<Class<? extends Throwable>> nonRetryableExceptions) {
            this.nonRetryableExceptions = nonRetryableExceptions;
            return this;
        }

        public ClientConfig build() {
            return new ClientConfig(nonRetryableExceptions, circuitBreakerIgnoreFailures, timeOutMs, maxRetries,
                    circuitBreakerName);
        }
    }
}
