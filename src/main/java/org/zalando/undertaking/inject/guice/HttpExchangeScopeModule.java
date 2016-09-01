package org.zalando.undertaking.inject.guice;

import java.util.function.Consumer;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.inject.HttpExchangeScoped;

import com.google.common.collect.ImmutableList;

import com.google.inject.AbstractModule;
import com.google.inject.PrivateBinder;

public final class HttpExchangeScopeModule extends AbstractModule {

    private final Iterable<? extends Consumer<? super PrivateBinder>> internalModules;

    @SafeVarargs
    public HttpExchangeScopeModule(final Consumer<? super PrivateBinder>... internalModules) {
        this.internalModules = ImmutableList.copyOf(internalModules);
    }

    @Override
    public void configure() {

        // create and bind scope
        final InternalHttpExchangeScope scope = new InternalHttpExchangeScope();
        bindScope(HttpExchangeScoped.class, scope);
        bind(HttpExchangeScope.class).toInstance(scope::runScoped);

        // bind exposed stuff
        install(new PrivateHttpExchangeModule(internalModules));
    }

}
