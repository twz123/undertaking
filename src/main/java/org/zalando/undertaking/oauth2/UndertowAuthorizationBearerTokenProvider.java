package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.inject.Request;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import rx.Observable;
import rx.Single;

final class UndertowAuthorizationBearerTokenProvider implements Provider<Single<AccessToken>> {

    private static final String BEARER_PREFIX = "Bearer ";

    private final HeaderMap requestHeaders;

    @Inject
    public UndertowAuthorizationBearerTokenProvider(@Request final HeaderMap requestHeaders) {
        this.requestHeaders = requireNonNull(requestHeaders);
    }

    @Override
    public Single<AccessToken> get() {
        return Observable.fromCallable(this::getToken).cache().toSingle();
    }

    private AccessToken getToken() throws NoAccessTokenException, MalformedAccessTokenException {

        final String headerValue =
            Optional.ofNullable(requestHeaders.get(Headers.AUTHORIZATION)) //
                    .map(HeaderValues::peekFirst)                          //
                    .orElseThrow(NoAccessTokenException::new);

        if (!headerValue.startsWith(BEARER_PREFIX)) {
            throw new MalformedAccessTokenException();
        }

        return AccessToken.of(headerValue.substring(BEARER_PREFIX.length()));
    }
}
