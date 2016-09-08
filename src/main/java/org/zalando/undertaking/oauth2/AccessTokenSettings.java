package org.zalando.undertaking.oauth2;

import java.net.URI;

import java.util.Set;

/**
 * Provides configuration settings for access token retrieval.
 *
 * @since  0.0.3
 */
public interface AccessTokenSettings {

    /**
     * URL to request a new access token creation.
     */
    URI getAccessTokenEndpoint();

    /**
     * OAuth scopes to request the token with.
     */
    Set<String> getAccessTokenScopes();

    /**
     * Percentage value of the validity period when the token should be refreshed.
     */
    int getRefreshTokenPercentage();
}
