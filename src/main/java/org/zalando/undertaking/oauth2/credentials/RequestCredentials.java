package org.zalando.undertaking.oauth2.credentials;

import static java.util.Objects.requireNonNull;

/**
 * Composition of client and user credentials.
 */
public class RequestCredentials {

    private final ClientCredentials clientCredentials;

    private final UserCredentials userCredentials;

    public RequestCredentials(final ClientCredentials clientCredentials, final UserCredentials userCredentials) {
        this.clientCredentials = requireNonNull(clientCredentials);
        this.userCredentials = requireNonNull(userCredentials);
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }

    public UserCredentials getUserCredentials() {
        return userCredentials;
    }
}
