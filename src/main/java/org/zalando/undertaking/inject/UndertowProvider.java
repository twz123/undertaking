package org.zalando.undertaking.inject;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import io.undertow.Undertow;

/**
 * Provides an {@link Undertow} instance by applying a set of {@link UndertowConfigurer UndertowConfigurers} to it.
 */
public class UndertowProvider implements Provider<Undertow> {

    private final Set<UndertowConfigurer> configurers;

    @Inject
    public UndertowProvider(final Set<UndertowConfigurer> configurers) {
        this.configurers = requireNonNull(configurers);
    }

    @Override
    public Undertow get() {
        final Undertow.Builder builder = Undertow.builder();

        for (final UndertowConfigurer configurer : configurers) {
            configurer.accept(builder);
        }

        return builder.build();
    }

}
