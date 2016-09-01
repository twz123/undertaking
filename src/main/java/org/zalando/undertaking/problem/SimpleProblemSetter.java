package org.zalando.undertaking.problem;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stores problem data into member variables.
 */
abstract class SimpleProblemSetter<S extends ProblemSetter<S>> implements ProblemSetter<S> {

    private final Map<String, Object> data = new HashMap<>(8);
    private Optional<Throwable> error = Optional.empty();

    @Override
    public S setParameter(final String name, final String value) {
        data.put(requireNonNull(name), requireNonNull(value));
        return self();
    }

    @Override
    public S setError(final Throwable error) {
        this.error = Optional.of(error);
        return self();
    }

    protected Map<String, Object> getData() {
        return data;
    }

    protected Optional<Throwable> getError() {
        return error;
    }

    protected abstract S self();
}
