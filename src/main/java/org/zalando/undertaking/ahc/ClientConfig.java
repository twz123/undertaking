package org.zalando.undertaking.ahc;

import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

public class ClientConfig {
    private final Set<Class<? extends Throwable>> nonRetryableExceptions;
    private final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailure;
    private final long timeoutMillis;
    private final int maxRetries;
    private final String circuitBreakerName;

    private ClientConfig(final Set<Class<? extends Throwable>> nonRetryableExceptions,
            final Set<Class<? extends Throwable>> circuitBreakerIgnoreFailure, final long timeoutMillis,
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

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public static class Builder {
        private long timeoutMillis = 2000L;
        private int maxRetries = 1;
        private String circuitBreakerName = "unnamed";
        private Set<Class<? extends Throwable>> circuitBreakerIgnoreFailures = Collections.emptySet();
        private Set<Class<? extends Throwable>> nonRetryableExceptions = Collections.emptySet();

        private Builder() { }

        public Builder timeOutMs(final long timeoutMillis) {
            checkArgument(timeoutMillis > 0, "timeoutMillis expected to be greater than 0");
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder maxRetries(final int maxRetries) {
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
