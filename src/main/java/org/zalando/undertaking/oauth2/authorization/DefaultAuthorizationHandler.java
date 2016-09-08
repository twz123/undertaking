package org.zalando.undertaking.oauth2.authorization;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.oauth2.AuthenticationInfo;
import org.zalando.undertaking.oauth2.AuthenticationInfoPredicate;
import org.zalando.undertaking.oauth2.AuthenticationInfoSettings;
import org.zalando.undertaking.oauth2.BadTokenInfoException;
import org.zalando.undertaking.oauth2.MalformedAccessTokenException;
import org.zalando.undertaking.oauth2.NoAccessTokenException;
import org.zalando.undertaking.oauth2.TokenInfoRequestException;
import org.zalando.undertaking.problem.ProblemHandlerBuilder;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import io.undertow.Handlers;

import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import rx.Observable;
import rx.Single;

import rx.schedulers.Schedulers;

import rx.subjects.AsyncSubject;

import rx.subscriptions.CompositeSubscription;

/**
 * Default implementation for OAuth2 based HTTP request authorization. Uses a {@link ProblemHandlerBuilder} to create
 * {@code HttpHandlers} if authorization failed.
 */
public class DefaultAuthorizationHandler implements AuthorizationHandler {

    public interface Settings {

        /**
         * The OAuth realm to be reported to clients in {@code WWW-Authenticate} headers.
         */
        String getRealm();

        /**
         * Timeout for the whole authorization flow. If processing time exceeds this threshold, request processing is
         * aborted and an {@code Internal Server Error} is reported to clients.
         */
        Duration getTimeout();
    }

    private static final String FORBIDDEN_ERROR_DESCRIPTION =
        "The request requires higher privileges than provided by the access token.";

    private final Settings settings;
    private final HttpExchangeScope scope;
    private final Provider<Single<AuthenticationInfo>> authInfoProvider;
    private final Provider<ProblemHandlerBuilder> problemBuilder;
    private final AuthenticationInfoSettings authInfoSettings;

    @Inject
    public DefaultAuthorizationHandler(final Settings settings, final HttpExchangeScope scope,
            final Provider<Single<AuthenticationInfo>> authInfoProvider,
            final Provider<ProblemHandlerBuilder> problemBuilder, final AuthenticationInfoSettings authInfoSettings) {
        this.settings = requireNonNull(settings);
        this.scope = requireNonNull(scope);
        this.authInfoProvider = requireNonNull(authInfoProvider);
        this.problemBuilder = requireNonNull(problemBuilder);
        this.authInfoSettings = requireNonNull(authInfoSettings);
    }

    @Override
    public HttpHandler require(final Predicate<? super AuthenticationInfo> predicate, final HttpHandler next) {
        requireNonNull(predicate);
        requireNonNull(next);

        return exchange -> require(exchange, predicate, Single.just(next));
    }

    @Override
    public HttpHandler require(final Predicate<? super AuthenticationInfo> predicate,
            final Function<? super HttpServerExchange, ? extends Single<HttpHandler>> nextProvider) {

        requireNonNull(predicate);
        requireNonNull(nextProvider);

        return exchange -> {

            // create an observable from the provider and subscribe on computation scheduler
            final Single<HttpHandler> next =                            //
                Single.fromCallable(() -> nextProvider.apply(exchange)) //
                      .subscribeOn(Schedulers.computation())            //
                      .flatMap(single -> single);

            require(exchange, predicate, next);
        };
    }

    private void require(final HttpServerExchange exchange, final Predicate<? super AuthenticationInfo> predicate,
            final Single<HttpHandler> next) {
        final Predicate<AuthenticationInfo> combined = new ContainsOverrideHeaderPredicate(exchange.getRequestHeaders())
                .and(predicate);

        // Emits a HttpHandler if and only if the authz predicate rejected access to the resource, empty otherwise
        final Single<Optional<HttpHandler>> authz = authInfoProvider.get().map(authInfo ->
                    combined.test(authInfo) ? Optional.empty() : Optional.of(forbidden(authInfo, combined)));

        // Holder for the eager subscriptions to the underlying singles
        final class EagerSubscriptions {
            private final CompositeSubscription sub = new CompositeSubscription();

            final Observable<Optional<HttpHandler>> authzSubject = subscribe(sub, authz);
            final Observable<HttpHandler> nextSubject = subscribe(sub, next);

            void unsubscribe() {
                sub.unsubscribe();
            }

            private <T> Observable<T> subscribe(final CompositeSubscription sub, final Single<T> single) {
                final AsyncSubject<T> subject = AsyncSubject.create();
                sub.add(single.subscribe(subject));
                return subject.asObservable();
            }
        }

        final Single<HttpHandler> handlerSingle = Single.using(() -> new EagerSubscriptions(),
                subscriptions ->
                    subscriptions.authzSubject.flatMap(authzHandler -> {

                        // if an error handler is present for authz, choose that
                        if (authzHandler.isPresent()) {
                            return Observable.just(authzHandler.get());
                        }
                        // no error handler for authz, continue with next handler
                        else {
                            return subscriptions.nextSubject;
                        }
                    }).toSingle(),
                EagerSubscriptions::unsubscribe);

        // Postpone subscription until call stack returns out of Connectors.executeRootHandler
        exchange.dispatch(Runnable::run, () -> subscribe(handlerSingle, exchange));
    }

    private void subscribe(final Single<HttpHandler> handlerSingle, final HttpServerExchange exchange) {
        handlerSingle.timeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS).subscribe(

            // handle the request using the emitted handler
            handler ->
                executeRootHandler(xc -> {
                        try {
                            handler.handleRequest(exchange);
                        } catch (final Exception e) {
                            exchange.unDispatch();
                            handleError(e).handleRequest(exchange);
                        }
                    },
                    exchange),

            // or send an error
            error -> executeRootHandler(handleError(error), exchange));
    }

    private void executeRootHandler(final HttpHandler handler, final HttpServerExchange exchange) {
        Connectors.executeRootHandler(scope.scoped(handler), exchange);
    }

    private HttpHandler handleError(final Throwable e) {

        if (e instanceof BadTokenInfoException) {
            final BadTokenInfoException btie = (BadTokenInfoException) e;
            return unauthorized(btie.getError(), btie.getErrorDescription().orElse(btie.getError()));
        }

        if (e instanceof HystrixRuntimeException) {
            return handleHystrixError((HystrixRuntimeException) e);
        }

        if (e instanceof NoAccessTokenException) {
            return unauthorized("invalid_request", "No Authorization provided.");
        }

        if (e instanceof MalformedAccessTokenException) {
            return unauthorized("invalid_request", "Authorization information didn't contain an OAuth2 bearer token.");
        }

        if (e instanceof TokenInfoRequestException) {
            return badGateway(e);
        }

        return internalServerError(e);
    }

    private HttpHandler handleHystrixError(final HystrixRuntimeException e) {
        switch (e.getFailureType()) {

            case TIMEOUT :
            case SHORTCIRCUIT :
                return gatewayTimeout(e);

            case REJECTED_SEMAPHORE_EXECUTION :
            case REJECTED_THREAD_EXECUTION :
                return serviceUnavailable(e);

            default :
                return internalServerError(e);
        }
    }

    protected HttpHandler unauthorized(final String oauthError, final String message) {
        return Handlers.header(                             //
                problemBuilder.get()                        //
                .setDetail(message)                         //
                .setParameter("error", oauthError)          //
                .setParameter("error_description", message) //
                .build(StatusCodes.UNAUTHORIZED),           //
                Headers.WWW_AUTHENTICATE_STRING,            //
                getWwwAuthenticateHeaderValue(oauthError));
    }

    protected HttpHandler forbidden(final AuthenticationInfo info,
            final Predicate<? super AuthenticationInfo> predicate) {

        String errorDescription = FORBIDDEN_ERROR_DESCRIPTION;
        if (predicate instanceof AuthenticationInfoPredicate) {
            errorDescription =
                ((AuthenticationInfoPredicate) predicate).getErrorDescription(info) //
                                                         .orElse(errorDescription);
        }

        return Handlers.header(                                      //
                problemBuilder.get()                                 //
                .setDetail(errorDescription)                         //
                .setParameter("error", "insufficient_scope")         //
                .setParameter("error_description", errorDescription) //
                .build(StatusCodes.FORBIDDEN),                       //
                Headers.WWW_AUTHENTICATE_STRING,                     //
                getWwwAuthenticateHeaderValue("insufficient_scope"));
    }

    protected HttpHandler badGateway(final Throwable e) {
        return
            problemBuilder.get()                                                         //
                          .setError(e)                                                   //
                          .setDetail("Failed to communicate with a downstream service.") //
                          .build(StatusCodes.BAD_GATEWAY);
    }

    protected HttpHandler serviceUnavailable(final Throwable e) {
        return
            problemBuilder.get()                                                                  //
                          .setError(e)                                                            //
                          .setDetail("Unable to handle the request due to a temporary overload.") //
                          .build(StatusCodes.SERVICE_UNAVAILABLE);
    }

    private HttpHandler gatewayTimeout(final HystrixRuntimeException e) {
        return
            problemBuilder.get()                                                           //
                          .setError(e)                                                     //
                          .setDetail("Communication with a downstream service timed out.") //
                          .build(StatusCodes.GATEWAY_TIME_OUT);
    }

    protected HttpHandler internalServerError(final Throwable e) {
        return problemBuilder.get()       //
                             .setError(e) //
                             .build(StatusCodes.INTERNAL_SERVER_ERROR);
    }

    private String getWwwAuthenticateHeaderValue(final String error) {
        return String.format("Bearer realm=\"%s\", error=\"%s\"", settings.getRealm(), error);
    }

    class ContainsOverrideHeaderPredicate implements AuthenticationInfoPredicate {

        private final HeaderMap requestHeaders;

        public ContainsOverrideHeaderPredicate(final HeaderMap requestHeaders) {
            this.requestHeaders = requireNonNull(requestHeaders);
        }

        @Override
        public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
            return Optional.of(String.format("The request requires the scope [%s] to override a business partner.",
                        authInfoSettings.getBusinessPartnerIdOverrideScope()));
        }

        @Override
        public boolean test(final AuthenticationInfo authenticationInfo) {
            return !requestHeaders.contains(authInfoSettings.getBusinessPartnerIdOverrideHeader())
                    || authenticationInfo.getScopes().contains(authInfoSettings.getBusinessPartnerIdOverrideScope());
        }
    }
}
