package org.zalando.undertaking.oauth2.authorization;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.oauth2.AuthenticationInfo;
import org.zalando.undertaking.oauth2.AuthenticationInfoPredicate;
import org.zalando.undertaking.oauth2.AuthenticationInfoSettings;
import org.zalando.undertaking.oauth2.BadTokenInfoException;
import org.zalando.undertaking.oauth2.MalformedAccessTokenException;
import org.zalando.undertaking.oauth2.NoAccessTokenException;
import org.zalando.undertaking.oauth2.TokenInfoRequestException;
import org.zalando.undertaking.problem.ProblemHandlerBuilder;

import io.github.robwin.circuitbreaker.CircuitBreakerOpenException;

import io.reactivex.Flowable;
import io.reactivex.Single;

import io.undertow.Handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.SameThreadExecutor;
import io.undertow.util.StatusCodes;

/**
 * Default implementation for OAuth2 based HTTP request authorization. Uses a {@link ProblemHandlerBuilder} to create
 * {@code HttpHandlers} if authorization failed.
 */
public class DefaultAuthorizationHandler implements AuthorizationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorizationHandler.class);
    private static final String FORBIDDEN_ERROR_DESCRIPTION =
        "The request requires higher privileges than provided by the access token.";
    private final Settings settings;
    private final HttpExchangeScope scope;
    private final Provider<Single<AuthenticationInfo>> authInfoProvider;
    private final Provider<ProblemHandlerBuilder> problemBuilder;

    @Inject
    public DefaultAuthorizationHandler(final Settings settings, final HttpExchangeScope scope,
            final Provider<Single<AuthenticationInfo>> authInfoProvider,
            final Provider<ProblemHandlerBuilder> problemBuilder) {
        this.settings = requireNonNull(settings);
        this.scope = requireNonNull(scope);
        this.authInfoProvider = requireNonNull(authInfoProvider);
        this.problemBuilder = requireNonNull(problemBuilder);
    }

    @Override
    public HttpHandler require(final Predicate<? super AuthenticationInfo> predicate, final HttpHandler next) {
        requireNonNull(predicate);
        requireNonNull(next);

        final Flowable<HttpHandler> nextEmitter = Flowable.just(next);
        return exchange -> handleRequest(exchange, predicate, nextEmitter);
    }

    @Override
    public HttpHandler require(final Predicate<? super AuthenticationInfo> predicate,
            final Function<? super HttpServerExchange, ? extends Single<HttpHandler>> nextProvider) {
        requireNonNull(predicate);
        requireNonNull(nextProvider);

        return exchange ->
                handleRequest(exchange, predicate, Flowable.defer(() -> nextProvider.apply(exchange).toFlowable()));
    }

    private void handleRequest(final HttpServerExchange exchange, final Predicate<? super AuthenticationInfo> predicate,
            final Flowable<HttpHandler> nextEmitter) {

        final Predicate<? super AuthenticationInfo> authPredicate = //
            wrapBusinessPartnerOverride(predicate, exchange.getRequestHeaders());

        // HttpHandlers are supposed to set their own status codes.
        // This is the last resort if something gets wrong.
        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);

        final Flowable<HttpHandler> authEmitter =                                //
            authInfoProvider.get().toFlowable()                                  //
                            .filter(authInfo -> !authPredicate.test(authInfo))   //
                            .map(authInfo -> forbidden(authInfo, authPredicate)) //
                            .onErrorResumeNext(error -> {
                                return Flowable.just(handleAuthError(error));
                            });

        final Single<HttpHandler> handlerSingle =
            Flowable.concatEager(Arrays.asList(authEmitter, nextEmitter))                   //
                    .take(1)                                                                //
                    .singleOrError()                                                        //
                    .timeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS,
                        Single.error(new TimeoutException("Timed out while authorizing request."))) //
                    .onErrorResumeNext(error -> Single.just(internalServerError(error)));

        final String requestId = LOG.isTraceEnabled() ? Integer.toHexString(exchange.hashCode()) : null;
        if (requestId != null) {
            LOG.trace("Dispatching request [{}].", requestId);
        }

        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> subscribe(requestId, handlerSingle, exchange));
    }

    private void subscribe(final Object requestId, final Single<HttpHandler> handlerSingle,
            final HttpServerExchange exchange) {

        if (requestId != null) {
            LOG.trace("Subscribing for request [{}].", requestId);
        }

        handlerSingle.subscribe( //
            handler -> {
                if (requestId != null) {
                    LOG.trace("Executing for request [{}]: [{}]", requestId, handler);
                }

                exchange.dispatch(scope.scoped(handler));
            },
            error -> {
                if (requestId != null) {
                    LOG.error("Fatal error occured while processing request [{}]: [{}]", requestId, error);
                }

                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                }

                exchange.endExchange();
            });

        if (requestId != null) {
            LOG.trace("Subscribed for request [{}].", requestId);
        }
    }

    private HttpHandler handleAuthError(final Throwable e) {

        if (e instanceof BadTokenInfoException) {
            final BadTokenInfoException btie = (BadTokenInfoException) e;
            return unauthorized(btie.getError(), btie.getErrorDescription().orElse(btie.getError()));
        }

        if (e instanceof TimeoutException) {
            return gatewayTimeout(e);
        }

        if (e instanceof CircuitBreakerOpenException) {
            return gatewayTimeout(e);
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

    private HttpHandler gatewayTimeout(final Throwable e) {
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

    private Predicate<? super AuthenticationInfo> wrapBusinessPartnerOverride(
            final Predicate<? super AuthenticationInfo> predicate, final HeaderMap requestHeaders) {

        final String overrideHeader = settings.getBusinessPartnerIdOverrideHeader();
        if (overrideHeader == null || !requestHeaders.contains(overrideHeader)) {
            return predicate;
        }

        final String overrideScope = settings.getBusinessPartnerIdOverrideScope();
        return new BusinessPartnerOverridePredicate(overrideScope).and(predicate);
    }

    public interface Settings extends AuthenticationInfoSettings {

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

    private static final class BusinessPartnerOverridePredicate implements AuthenticationInfoPredicate {
        private static final Optional<String> ERROR_DESC = Optional.of(
                "The request is not authorized to override the business partner.");

        private final String requiredScope;

        BusinessPartnerOverridePredicate(final String requiredScope) {
            this.requiredScope = requiredScope;
        }

        @Override
        public boolean test(final AuthenticationInfo authInfo) {

            // if, for some reason, the required scope is null, reject all requests due to security considerations
            return requiredScope != null && authInfo.getScopes().contains(requiredScope);
        }

        @Override
        public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
            return ERROR_DESC;
        }
    }
}
