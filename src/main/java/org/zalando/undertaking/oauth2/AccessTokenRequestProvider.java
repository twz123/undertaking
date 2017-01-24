package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.MoreObjects.firstNonNull;

import static javaslang.API.*;

import static javaslang.Predicates.instanceOf;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.asynchttpclient.*;

import org.asynchttpclient.extras.rxjava2.single.AsyncHttpSingle;

import org.zalando.undertaking.oauth2.credentials.ClientCredentials;
import org.zalando.undertaking.oauth2.credentials.RequestCredentials;
import org.zalando.undertaking.oauth2.credentials.UserCredentials;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import io.github.robwin.circuitbreaker.operator.CircuitBreakerOperator;

import io.reactivex.Single;

import io.reactivex.functions.BiPredicate;

import io.undertow.util.StatusCodes;

class AccessTokenRequestProvider extends OAuth2RequestProvider {

    private final Clock clock;
    private final CircuitBreaker circuitBreaker;
    private final AccessTokenSettings settings;

    @Inject
    public AccessTokenRequestProvider(final AccessTokenSettings settings, final AsyncHttpClient client,
            final Clock clock, final CircuitBreakerRegistry circuitBreakerRegistry) {
        super(client);
        this.settings = requireNonNull(settings);
        this.clock = requireNonNull(clock);

        CircuitBreakerConfig config =
            CircuitBreakerConfig
                .custom().recordFailure(e -> Match(e).of(
                    Case(instanceOf(BadAccessTokenException.class), false),
                    Case($(), true)))
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("auth/accessToken", config);
    }

    private static Realm createRealm(final ClientCredentials credentials) {
        return new Realm.Builder(credentials.getClientId(), credentials.getClientSecret()).setUsePreemptiveAuth(true)
                                                                                          .setScheme(
                Realm.AuthScheme.BASIC).build();
    }

    private BoundRequestBuilder createRequestBuilder(final RequestCredentials credentials) {
        return
            httpClient.preparePost(settings.getAccessTokenEndpoint().toString()) //
                      .setRealm(createRealm(credentials.getClientCredentials())) //
                      .setHeader(HttpHeaders.ACCEPT, "application/json")         //
                      .addQueryParam("realm", "/services")                       //
                      .setFormParams(createFormParams(credentials.getUserCredentials()));
    }

    public Single<AccessTokenResponse> requestAccessToken(final RequestCredentials credentials) {
        final Single<Instant> requestTimeProvider = Single.fromCallable(clock::instant);

        return createRequest(createRequestBuilder(credentials))                                //
                .map(AccessTokenRequestProvider.this::parsePayload)                            //
                .zipWith(requestTimeProvider, this::buildResponse)                             //
                .retry(maxRetriesOr(3,  e -> Match(e).of(                                      //
                                Case(instanceOf(BadAccessTokenException.class), false),        //
                                Case($(), true))))                                             //
                .timeout(10_000, TimeUnit.MILLISECONDS, Single.error(new TimeoutException()))  //
                .lift(CircuitBreakerOperator.of(circuitBreaker));                              //
    }

    @VisibleForTesting
    @SuppressWarnings("static-method")
    Single<Response> createRequest(final BoundRequestBuilder requestBuilder) {
        return AsyncHttpSingle.create(requestBuilder);
    }

    private Payload parsePayload(final Response response) {
        final int statusCode = response.getStatusCode();

        switch (statusCode) {

            case StatusCodes.OK :

                final Payload payload = parse(response.getResponseBody(), Payload.class);
                if (payload == null) {
                    throw new AccessTokenRequestException("No payload for OK response");
                }

                if (payload.accessToken == null) {
                    throw new AccessTokenRequestException("No access_token in response");
                }

                return payload;

            case StatusCodes.BAD_REQUEST :
            case StatusCodes.UNAUTHORIZED :

                final ErrorPayload errorPayload;
                try {
                    errorPayload = parse(response.getResponseBody(), ErrorPayload.class);

                    throw new BadAccessTokenException(errorPayload == null ? null : errorPayload.error,
                        errorPayload == null ? null : errorPayload.errorDescription);
                } catch (final AccessTokenRequestException e) {
                    throw new BadAccessTokenException("HTTP status code " + statusCode, firstNonNull(e.getCause(), e));
                }
        }

        throw new AccessTokenRequestException("Unexpected status code: " + statusCode + ": "
                + response.getResponseBody());
    }

    private AccessTokenResponse buildResponse(final Payload payload, final Instant requestTime) {
        return new AccessTokenResponse(AccessToken.bearer(payload.accessToken),
                requestTime.plus(payload.expiresIn, ChronoUnit.SECONDS));
    }

    private List<Param> createFormParams(final UserCredentials credentials) {
        return ImmutableList.of(new Param("grant_type", "password"),
                new Param("username", credentials.getApplicationUsername()),
                new Param("password", credentials.getApplicationPassword()),
                new Param("scope", Joiner.on(' ').join(settings.getAccessTokenScopes())));
    }

    private BiPredicate<Integer, Throwable> maxRetriesOr(final int maxRetries, final Predicate<Throwable> pred) {
        return (tries, ex) -> tries < maxRetries && pred.test(ex);
    }

    private static final class Payload {

        long expiresIn;
        String tokenType;
        String realm;
        String scope;
        String grantType;
        String uid;
        String accessToken;
    }

}
