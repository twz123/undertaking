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
     */
    String getBusinessPartnerIdOverrideHeader();

    /**
     * Name of scope being required to overwrite the business partner identifier.
     */
    String getBusinessPartnerIdOverrideScope();
}
