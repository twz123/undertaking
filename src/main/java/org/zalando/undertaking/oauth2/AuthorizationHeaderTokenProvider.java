package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.inject.Request;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import rx.Single;

public final class AuthorizationHeaderTokenProvider implements Provider<Single<AccessToken>> {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Single<AccessToken> NO_TOKEN = Single.error(new NoAccessTokenException());
    private static final Single<AccessToken> MALFORMED_TOKEN = Single.error(new MalformedAccessTokenException());

    private final HeaderMap requestHeaders;

    @Inject
    public AuthorizationHeaderTokenProvider(@Request final HeaderMap requestHeaders) {
        this.requestHeaders = requireNonNull(requestHeaders);
    }

    @Override
    public Single<AccessToken> get() {
        return
            Optional.ofNullable(requestHeaders.get(Headers.AUTHORIZATION))                                 //
                    .map(HeaderValues::peekFirst)                                                          //
                    .map(value -> value.startsWith(BEARER_PREFIX) ? extractToken(value) : MALFORMED_TOKEN) //
                    .orElse(NO_TOKEN);
    }

    private static Single<AccessToken> extractToken(final String value) {
        return Single.just(AccessToken.of(value.substring(BEARER_PREFIX.length())));
    }
}
