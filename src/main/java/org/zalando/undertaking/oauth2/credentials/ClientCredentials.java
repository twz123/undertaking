package org.zalando.undertaking.oauth2.credentials;

/**
 * Pair of client username and password to authenticate against the access token endpoint.
 */
@SuppressWarnings("unused")
public class ClientCredentials {

    private String clientId;

    private String clientSecret;

    public ClientCredentials() { }

    public ClientCredentials(final String clientId, final String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
