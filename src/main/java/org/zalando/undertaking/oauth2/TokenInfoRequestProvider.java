package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;

import static javaslang.Predicates.instanceOf;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import org.asynchttpclient.extras.rxjava2.single.AsyncHttpSingle;

import com.google.common.net.HttpHeaders;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import io.github.robwin.circuitbreaker.operator.CircuitBreakerOperator;

import io.reactivex.Single;

import io.reactivex.functions.BiPredicate;

import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

class TokenInfoRequestProvider extends OAuth2RequestProvider {

    private final CircuitBreaker circuitBreaker;
    private final AuthenticationInfoSettings settings;

    @Inject
    public TokenInfoRequestProvider(final AuthenticationInfoSettings settings, final AsyncHttpClient client,
            final CircuitBreakerRegistry circuitBreakerRegistry) {
        super(client);
        this.settings = requireNonNull(settings);

        CircuitBreakerConfig config =                                             //
            CircuitBreakerConfig.custom()                                         //
                                .recordFailure(e ->
                                        Match(e).of(                              //
                                            Case(instanceOf(BadAccessTokenException.class), false), //
                                            Case(instanceOf(TokenInfoRequestException.class), false), //
                                            Case($(), true))).build();            //
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("auth/accessToken", config);
    }

    private static <T> Single<T> mapError(final Throwable error) {
        return Single.error(new TokenInfoRequestException(error.getMessage(), error));
    }

    public Single<AuthenticationInfo> getTokenInfo(final AccessToken accessToken, final HeaderMap requestHeaders) {

        return this.construct(buildRequest(accessToken), requestHeaders).timeout(10_000, TimeUnit.MILLISECONDS) //
                   .retry(maxRetriesOr(3,
                           e ->
                               Match(e).of(                                                                     //
                                   Case(instanceOf(BadAccessTokenException.class), false),                      //
                                   Case(instanceOf(TokenInfoRequestException.class), false),                    //
                                   Case($(), true))))                                                           //
                   .lift(CircuitBreakerOperator.of(circuitBreaker));                                            //
    }

    private BoundRequestBuilder buildRequest(final AccessToken accessToken) {
        return
            httpClient.prepareGet(settings.getTokenInfoEndpoint().toString()) //
                      .setHeader(HttpHeaders.ACCEPT, "application/json")      //
                      .addQueryParam("access_token", accessToken.getValue());
    }

    private Single<AuthenticationInfo> construct(final BoundRequestBuilder requestBuilder,
            final HeaderMap requestHeaders) {
        return AsyncHttpSingle.create(requestBuilder).onErrorResumeNext(TokenInfoRequestProvider::mapError).map(
                response -> parseResponse(response, requestHeaders));
    }

    private AuthenticationInfo parseResponse(final Response response, final HeaderMap requestHeaders) {
        final int statusCode = response.getStatusCode();
        switch (statusCode) {

            case StatusCodes.OK :

                final Payload payload = parse(response.getResponseBody(), Payload.class);
                return
                    AuthenticationInfo.builder()                                           //
                                      .uid(payload.uid)                                    //
                                      .scopes(payload.scope)                               //
                                      .businessPartnerId(
                                          Optional.ofNullable(settings.getBusinessPartnerIdOverrideHeader()) //
                                          .map(requestHeaders::getFirst).orElse(null))     //
                                      .build();

            case StatusCodes.BAD_REQUEST :
            case StatusCodes.UNAUTHORIZED :

                final ErrorPayload errorPayload = parse(response.getResponseBody(), ErrorPayload.class);

                // Fix wrong error from Zalando endpoint
                if ("invalid_request".equals(errorPayload.error)
                        && "Access Token not valid".equals(errorPayload.errorDescription)) {
                    throw new BadTokenInfoException("invalid_token", "Access Token not valid");
                }

                throw new BadTokenInfoException(errorPayload.error, errorPayload.errorDescription);
        }

        throw new TokenInfoRequestException("Unsupported status code: " + statusCode + ": "
                + response.getResponseBody());
    }

    private BiPredicate<Integer, Throwable> maxRetriesOr(final int maxRetries, final Predicate<Throwable> pred) {
        return (tries, ex) -> tries < maxRetries && pred.test(ex);
    }

    private static final class Payload {
        Set<String> scope;
        String uid;
    }
}
