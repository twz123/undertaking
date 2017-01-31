package org.zalando.undertaking.config;

import java.util.Arrays;
import java.util.Collections;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.binder.ScopedBindingBuilder;

/**
 * Creates bindings for configuration interfaces to be dynamically implemented by the {@code owner} library.
 */
public final class ConfigInterfaceModule extends AbstractModule {
    private final Iterable<? extends Class<? extends Config>> configInterfaces;

    public static <T extends Config> Module with(final Class<T> configInterface) {
        return with(Collections.singletonList(configInterface));
    }

    public static Module with(final Class<? extends Config>... configInterfaces) {
        return with(Arrays.asList(configInterfaces));
    }

    public static Module with(final Iterable<? extends Class<? extends Config>> configInterfaces) {
        return new ConfigInterfaceModule(configInterfaces);
    }

    private ConfigInterfaceModule(final Iterable<? extends Class<? extends Config>> configInterfaces) {
        this.configInterfaces = configInterfaces;
    }

    @Override
    protected void configure() {
        for (final Class<? extends Config> configInterface : configInterfaces) {
            bindConfigInterface(configInterface).in(Singleton.class);
        }
    }

    private <T extends Config> ScopedBindingBuilder bindConfigInterface(final Class<T> configInterface) {
        return bind(configInterface).toProvider(() -> ConfigFactory.create(configInterface, System.getenv()));
    }
}
