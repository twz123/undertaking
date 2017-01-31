package org.zalando.undertaking.config;

import java.util.Arrays;
import java.util.Collections;

import com.google.inject.Binder;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.binder.ScopedBindingBuilder;

import static java.util.Objects.requireNonNull;

/**
 * <p>
 *   Creates bindings for configuration interfaces to be dynamically implemented by the {@code owner} library.
 * </p>
 *
 * <p>
 *  The configuration is set up in a way that it automatically resolves system environment properties in addition to
 *  resolving any <code>{@literal @}LoadStrategy</code> annotations set on the <code>Configuration</code> interfaces.
 * </p>
 *
 * Usage:
 * <ul>
 *  <li>Define an Interface for holding the configuration, see <a href="http://owner.aeonbits.org/docs/usage/">Owner Usage Documentation</a></li>
 *  <li>Use this class in a Guice module to bind the interface</li>
 *  <li>Use <code>@Inject</code> to get injected instances of the configuration.</li>
 * </ul>
 *
 * Example, assuming the <code>ServerConfig</code> interface from the Usage Documentation:
 * <pre>
 *     public class TestModule extends Module {
 *        {@literal @}Override
 *         public void configure(Binder binder) {
 *             install(ConfigInterfaceModule.with(ServerConfig.class));
 *         }
 *     }
 *
 *     public class MyTestConsumer {
 *        {@literal @}Inject
 *         private ServerConfig config;
 *
 *         // rest omitted for brevity
 *     }
 * </pre>
 */
public final class ConfigInterfaceModule extends AbstractModule {
    private final Iterable<? extends Class<? extends Config>> configInterfaces;

    public static Module with(final Class<? extends Config> configInterface) {
        return with(Collections.singletonList(configInterface));
    }

    public static Module with(final Class<? extends Config>... configInterfaces) {
        return with(Arrays.asList(configInterfaces));
    }

    public static Module with(final Iterable<? extends Class<? extends Config>> configInterfaces) {
        return new ConfigInterfaceModule(configInterfaces);
    }

    private ConfigInterfaceModule(final Iterable<? extends Class<? extends Config>> configInterfaces) {
        this.configInterfaces = requireNonNull(configInterfaces);
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
