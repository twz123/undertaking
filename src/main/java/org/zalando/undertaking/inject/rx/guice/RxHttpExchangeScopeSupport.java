package org.zalando.undertaking.inject.rx.guice;

import org.zalando.undertaking.inject.rx.RxHttpExchangeScope;

import com.google.inject.PrivateBinder;

public final class RxHttpExchangeScopeSupport {

    public static void install(final PrivateBinder binder) {
        binder.bind(RxHttpExchangeScope.class).to(InternalRxHttpExchangeScope.class);
        binder.expose(RxHttpExchangeScope.class);

    }

    private RxHttpExchangeScopeSupport() {
        throw new AssertionError("No instances for you!");
    }
}
