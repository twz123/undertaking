package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

final class AccessTokenResponse {

    private final AccessToken accessToken;
    private final Instant expiryTime;

    public AccessTokenResponse(final AccessToken accessToken, final Instant expiryTime) {
        this.accessToken = requireNonNull(accessToken);
        this.expiryTime = requireNonNull(expiryTime);
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }
}
