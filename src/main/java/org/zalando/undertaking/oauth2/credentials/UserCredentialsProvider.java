package org.zalando.undertaking.oauth2.credentials;

import javax.inject.Inject;

/**
 * Provides credential instances of type {@code UserCredentials}.
 */
class UserCredentialsProvider extends CredentialsProvider<UserCredentials> {

    /**
     * Name of the file containing the credentials.
     */
    public static final String FILENAME = "user.json";

    /**
     * Creates a new provider to load credentials of type {@code UserCredentials} from a specific file.
     *
     * @see  CredentialsProvider#CredentialsProvider(String, CredentialsSettings)
     */
    @Inject
    public UserCredentialsProvider(final CredentialsSettings settings) {
        super(FILENAME, settings);
    }
}
