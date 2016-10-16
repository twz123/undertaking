package org.zalando.undertaking.inject;

import static org.mockito.ArgumentMatchers.notNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collections;

import org.junit.Test;

public class UndertowServiceProviderTest {

    @Test
    public void callsConfigurerOnce() {
        final UndertowConfigurer configurer = mock(UndertowConfigurer.class);
        final UndertowServiceProvider underTest = new UndertowServiceProvider(Collections.singleton(configurer));

        underTest.get();
        verify(configurer).accept(notNull());
        verifyNoMoreInteractions(configurer);
    }

}
