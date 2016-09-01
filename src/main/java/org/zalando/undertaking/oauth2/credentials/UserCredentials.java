package org.zalando.undertaking.oauth2.credentials;

/**
 * Pair of username and password to request the actual access token for.
 */
@SuppressWarnings("unused")
public class UserCredentials {

    private String applicationUsername;

    private String applicationPassword;

    public String getApplicationUsername() {
        return applicationUsername;
    }

    public String getApplicationPassword() {
        return applicationPassword;
    }
}
