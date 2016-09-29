package org.zalando.undertaking.oauth2;

import java.net.URI;

/**
 * Provides configuration settings to request token information.
 *
 * @since  0.0.3
 */
public interface AuthenticationInfoSettings {

    /**
     * URL to validate and request information about access tokens.
     */
    URI getTokenInfoEndpoint();

    /**
     * Name of the HTTP header being used to overwrite the business partner identifier.
     *
     * @return  the header to use to look for business partner overrides, or {@code null} if this feature is disabled
     */
    String getBusinessPartnerIdOverrideHeader();

    /**
     * Name of scope being required to overwrite the business partner identifier.
     *
     * @return  the required scope, {@code null} means that no request is permitted to override the business partner
     */
    String getBusinessPartnerIdOverrideScope();
}
