package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import org.zalando.undertaking.ahc.ClientConfig;
import org.zalando.undertaking.ahc.GuardedHttpClient;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;

import io.reactivex.Single;

import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

class TokenInfoRequestProvider extends OAuth2RequestProvider {

    private final AuthenticationInfoSettings settings;
    private final GuardedHttpClient guardedHttpClient;

    private final ClientConfig requestConfig = ClientConfig.builder().circuitBreakerName("auth/tokenInfo").maxRetries(3)
                                                           .circuitBreakerIgnoreFailures(ImmutableSet.of(
                                                                   BadAccessTokenException.class))
                                                           .nonRetryableExceptions(ImmutableSet.of(
                BadAccessTokenException.class)).timeOutMs(10_000L).build();

    @Inject
    public TokenInfoRequestProvider(final AuthenticationInfoSettings settings, final AsyncHttpClient client,
            final GuardedHttpClient guardedHttpClient) {
        super(client);
        this.settings = requireNonNull(settings);
        this.guardedHttpClient = requireNonNull(guardedHttpClient);
    }

    private static <T> Single<T> mapError(final Throwable error) {
        return Single.error(new TokenInfoRequestException(error.getMessage(), error));
    }

    public Single<AuthenticationInfo> getTokenInfo(final AccessToken accessToken, final HeaderMap requestHeaders) {
        return guardedHttpClient.executeRequest(buildRequest(accessToken),
                                    response -> parseResponse(response, requestHeaders),
                                    requestConfig).onErrorResumeNext(TokenInfoRequestProvider::mapError);
    }

    private BoundRequestBuilder buildRequest(final AccessToken accessToken) {
        return
            httpClient.prepareGet(settings.getTokenInfoEndpoint().toString()) //
                      .setHeader(HttpHeaders.ACCEPT, "application/json")      //
                      .addQueryParam("access_token", accessToken.getValue());
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

    private static final class Payload {
        Set<String> scope;
        String uid;
    }
}
