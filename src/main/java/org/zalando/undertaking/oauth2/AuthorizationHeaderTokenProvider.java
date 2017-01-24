package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.inject.Request;

import io.reactivex.Single;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

public final class AuthorizationHeaderTokenProvider implements Provider<Single<AccessToken>> {

    private static final Single<AccessToken> NO_TOKEN = Single.error(new NoAccessTokenException());

    private final HeaderMap requestHeaders;

    @Inject
    public AuthorizationHeaderTokenProvider(@Request final HeaderMap requestHeaders) {
        this.requestHeaders = requireNonNull(requestHeaders);
    }

    @Override
    public Single<AccessToken> get() {
        return
            Optional.ofNullable(requestHeaders.get(Headers.AUTHORIZATION)) //
                    .map(HeaderValues::peekFirst)                          //
                    .map(AccessToken::parse)                               //
                    .map(Single::just)                                     //
                    .orElse(NO_TOKEN);
    }
}
