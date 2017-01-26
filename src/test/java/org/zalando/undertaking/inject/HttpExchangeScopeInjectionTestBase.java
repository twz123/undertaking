package org.zalando.undertaking.inject;

import java.util.function.Consumer;

import javax.inject.Inject;

import org.zalando.undertaking.inject.guice.HttpExchangeScopeModule;
import org.zalando.undertaking.inject.rx.guice.RxHttpExchangeScopeSupport;

import com.google.inject.*;
import com.google.inject.name.Names;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;

public class HttpExchangeScopeInjectionTestBase {
    protected <T extends HttpHandler> T getNamedHandler(final Class<T> type, final String name,
            final Injector injector) {
        return type.cast(injector.getInstance(namedHandler(name)));
    }

    protected Key<HttpHandler> namedHandler(final String name) {
        return Key.get(HttpHandler.class, Names.named(name));
    }

    protected Injector getInjector() {
        Consumer subModule = (Consumer<PrivateBinder>) binder -> {

            binder.bind(HttpHandler.class)                   //
                  .annotatedWith(Names.named("testhandler")) //
                  .to(TestSimpleHandler.class)               //
                  .in(HttpExchangeScoped.class);             //

            binder.bind(HttpHandler.class)                              //
                  .annotatedWith(Names.named("testAllInjectedHandler")) //
                  .to(TestAllInjectedHandler.class)                     //
                  .in(HttpExchangeScoped.class);                        //

            binder.expose(namedHandler("testhandler"));
            binder.expose(namedHandler("testAllInjectedHandler"));
        };

        return Guice.createInjector((Module) binder -> {
                    binder.install(new HttpExchangeScopeModule(subModule, RxHttpExchangeScopeSupport::install));
                });
    }

    protected static class TestSimpleHandler implements HttpHandler {
        @Inject
        @Request
        private HeaderMap headerMap;

        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {
            // noop
        }

        public HeaderMap getHeaderMap() {
            return headerMap;
        }
    }

    protected static class TestAllInjectedHandler extends TestSimpleHandler {
        @Inject
        private HttpExchangeScopedExecutor exchangeScopedExecutor;

        @Inject
        private HttpServerExchange exchange;

        public HttpExchangeScopedExecutor getExchangeScopedExecutor() {
            return exchangeScopedExecutor;
        }

        public HttpServerExchange getExchange() {
            return exchange;
        }
    }
}
