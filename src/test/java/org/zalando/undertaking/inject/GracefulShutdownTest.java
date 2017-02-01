package org.zalando.undertaking.inject;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

import javax.annotation.PreDestroy;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import io.undertow.server.HttpHandler;

@RunWith(MockitoJUnitRunner.class)
public class GracefulShutdownTest {

    @Mock
    private HttpHandler next;

    private final GracefulShutdown underTest = new GracefulShutdown();

    @Test
    public void ensurePreDestroyIsPresent() throws Exception {
        assertThat(GracefulShutdown.class.getMethod("close").getAnnotation(PreDestroy.class),
            is(instanceOf(PreDestroy.class)));

    }

    @Test(expected = IllegalStateException.class)
    public void illegalStateExceptionWhenWrapAfterClose() throws InterruptedException {
        underTest.close();
        underTest.wrap(next);
    }
}
