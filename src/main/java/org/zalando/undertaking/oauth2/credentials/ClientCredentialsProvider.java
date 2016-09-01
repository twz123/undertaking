package org.zalando.undertaking.oauth2.credentials;

import javax.inject.Inject;

/**
 * Provides credential instances of type {@code ClientCredentials}.
 */
class ClientCredentialsProvider extends CredentialsProvider<ClientCredentials> {

    /**
     * Name of the file containing the credentials.
     */
    public static final String FILENAME = "client.json";

    /**
     * Creates a new provider to load credentials of type {@code ClientCredentials} from a specific file.
     *
     * @see  CredentialsProvider#CredentialsProvider(String, CredentialsSettings)
     */
    @Inject
    public ClientCredentialsProvider(final CredentialsSettings settings) {
        super(FILENAME, settings);
    }
}
