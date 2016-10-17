package org.zalando.undertaking.inject;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PreDestroy;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

/**
 * Wraps {@code HttpHandlers} in order to provide graceful shutdown behavior. May be used as a in DI contexts for
 * lifecycle management.
 */
public class GracefulShutdown implements HandlerWrapper, AutoCloseable {

    private Collection<GracefulShutdownHandler> handlers = new ArrayList<>(4);

    private volatile boolean destroyed;

    @Override
    public HttpHandler wrap(final HttpHandler next) {
        checkState(!destroyed, "Graceful shutdown already initiated!");

        synchronized (handlers) {
            checkState(!destroyed, "Graceful shutdown already initiated!");

            final GracefulShutdownHandler wrapper = new GracefulShutdownHandler(next);
            handlers.add(wrapper);
            return wrapper;
        }
    }

    @Override
    @PreDestroy
    public void close() throws InterruptedException {
        if (destroyed) {
            return;
        }

        synchronized (handlers) {
            if (destroyed) {
                return;
            }

            handlers.forEach(GracefulShutdownHandler::shutdown);
            for (final GracefulShutdownHandler handler : handlers) {
                handler.awaitShutdown();
            }

            destroyed = true;
            handlers = null;
        }
    }
}
