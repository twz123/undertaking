package org.zalando.undertaking.oauth2;

import java.net.URI;

import java.util.Set;

/**
 * Provides configuration settings for the OAuth2 functionality.
 */
public interface OAuth2Settings {

    /**
     * URL to request a new access token creation.
     */
    URI getAccessTokenEndpoint();

    /**
     * URL to validate and request information about access tokens.
     */
    URI getTokenInfoEndpoint();

    /**
     * OAuth scopes to request the token with.
     */
    Set<String> getAccessTokenScopes();

    /**
     * Percentage value of the validity period when the token should be refreshed.
     */
    int getRefreshTokenPercentage();

    /**
     * Name of the HTTP header being used to overwrite the business partner identifier.
     */
    String getBusinessPartnerIdOverrideHeader();

    /**
     * Name of scope being required to overwrite the business partner identifier.
     */
    String getBusinessPartnerIdOverrideScope();
}
