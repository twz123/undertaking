package org.zalando.undertaking.inject;

import java.util.function.Consumer;

import io.undertow.Undertow;

/**
 * Callback to configure an {@link Undertow} instance during its bootstrapping phase.
 */
@FunctionalInterface
public interface UndertowConfigurer extends Consumer<Undertow.Builder> {

    /**
     * Configures {@code builder} according to this configurer.
     */
    @Override
    void accept(Undertow.Builder builder);

}
