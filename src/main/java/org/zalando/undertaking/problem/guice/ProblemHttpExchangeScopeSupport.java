package org.zalando.undertaking.problem.guice;

import javax.inject.Singleton;

import org.zalando.undertaking.problem.Internal;
import org.zalando.undertaking.problem.ProblemDefaultResponseHandler;
import org.zalando.undertaking.problem.ProblemHandlerBuilder;
import org.zalando.undertaking.problem.ProblemRecorder;

import com.google.gson.Gson;

import com.google.inject.PrivateBinder;

/**
 * Adds support for {@code application/problem+json}.
 */
public final class ProblemHttpExchangeScopeSupport {

    public static void install(final PrivateBinder binder) {
        binder.bind(Gson.class).annotatedWith(Internal.class).to(Gson.class).in(Singleton.class);

        binder.bind(ProblemDefaultResponseHandler.class);
        binder.expose(ProblemDefaultResponseHandler.class);

        binder.bind(ProblemHandlerBuilder.class);
        binder.expose(ProblemHandlerBuilder.class);

        binder.bind(ProblemRecorder.class);
        binder.expose(ProblemRecorder.class);
    }

    private ProblemHttpExchangeScopeSupport() {
        throw new AssertionError("No instances for you!");
    }
}
