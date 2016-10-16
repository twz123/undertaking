package org.zalando.undertaking.inject;

import static java.util.Objects.requireNonNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import javax.inject.Inject;

import io.undertow.Undertow;

/**
 * Utility class that may be used in DI contexts to get lifecycle support for {@code Undertow} instances.
 */
public final class UndertowService {

    private final Undertow server;

    @Inject
    public UndertowService(final Undertow server) {
        this.server = requireNonNull(server);
    }

    @PostConstruct
    void start() {
        server.start();
    }

    @PreDestroy
    void stop() {
        server.stop();
    }
}
