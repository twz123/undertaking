package org.zalando.undertaking.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.aeonbits.owner.Config;

import org.junit.Rule;
import org.junit.Test;

import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ConfigInterfaceModuleTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    public interface TestConfig extends Config {
        @Key("TEST_KEY")
        String getTestKey();
    }

    public interface TestSecondConfig extends Config {
        @Key("ANOTHER_TEST_KEY")
        String getAnotherTestKey();
    }

    @Test
    public void canInjectConfigBasedOnEnvironmentVariables() {
        environmentVariables.set("TEST_KEY", "hello");

        Injector injector = Guice.createInjector(ConfigInterfaceModule.with(TestConfig.class));

        TestConfig testConfig = injector.getInstance(TestConfig.class);
        assertThat(testConfig.getTestKey()).isEqualTo("hello");
    }

    @Test
    public void canInjectTwoConfigs() {
        environmentVariables.set("TEST_KEY", "hello");
        environmentVariables.set("ANOTHER_TEST_KEY", "world");

        @SuppressWarnings("unchecked")
        Module configModule = ConfigInterfaceModule.with(TestConfig.class, TestSecondConfig.class);
        Injector injector = Guice.createInjector(configModule);

        TestConfig testConfig = injector.getInstance(TestConfig.class);
        assertThat(testConfig.getTestKey()).isEqualTo("hello");

        TestSecondConfig testSecondConfig = injector.getInstance(TestSecondConfig.class);
        assertThat(testSecondConfig.getAnotherTestKey()).isEqualTo("world");
    }
}
