package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;

import javax.inject.Inject;

import org.asynchttpclient.*;

import org.zalando.undertaking.ahc.ClientConfig;
import org.zalando.undertaking.ahc.GuardedHttpClient;
import org.zalando.undertaking.oauth2.credentials.ClientCredentials;
import org.zalando.undertaking.oauth2.credentials.RequestCredentials;
import org.zalando.undertaking.oauth2.credentials.UserCredentials;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;

import io.reactivex.Single;

import io.undertow.util.StatusCodes;

class AccessTokenRequestProvider extends OAuth2RequestProvider {
    private final Clock clock;
    private final AccessTokenSettings settings;

    private final ClientConfig requestConfig = ClientConfig.builder().circuitBreakerName("auth/accessToken")
                                                           .maxRetries(3)
                                                           .circuitBreakerIgnoreFailures(ImmutableSet.of(
                                                                   BadAccessTokenException.class))
                                                           .nonRetryableExceptions(ImmutableSet.of(
                BadAccessTokenException.class)).timeOutMs(10_000L).build();

    private final GuardedHttpClient guardedHttpClient;

    @Inject
    public AccessTokenRequestProvider(final AccessTokenSettings settings, final AsyncHttpClient client,
            final Clock clock, final GuardedHttpClient guardedHttpClient) {

        super(client);
        this.settings = requireNonNull(settings);
        this.clock = requireNonNull(clock);
        this.guardedHttpClient = requireNonNull(guardedHttpClient);
    }

    public Single<AccessTokenResponse> requestAccessToken(final RequestCredentials credentials) {
        return guardedHttpClient.executeRequest(createRequestBuilder(credentials), this::handleResponse, requestConfig);
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

    private AccessTokenResponse handleResponse(final Response response) {
        return buildResponse(parsePayload(response), clock.instant());
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
