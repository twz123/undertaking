package org.zalando.undertaking.inject;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import io.undertow.Undertow;

/**
 * Provides an {@link UndertowService} by applying a set of {@link UndertowConfigurer UndertowConfigurers} to it.
 */
public class UndertowServiceProvider implements Provider<UndertowService> {

    private final Set<UndertowConfigurer> configurers;

    @Inject
    UndertowServiceProvider(final Set<UndertowConfigurer> configurers) {
        this.configurers = requireNonNull(configurers);
    }

    @Override
    public UndertowService get() {
        final Undertow.Builder builder = Undertow.builder();

        for (final UndertowConfigurer configurer : configurers) {
            configurer.accept(builder);
        }

        return new UndertowService(builder.build());
    }
}
