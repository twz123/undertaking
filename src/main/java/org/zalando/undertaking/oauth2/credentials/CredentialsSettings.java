package org.zalando.undertaking.oauth2.credentials;

import java.nio.file.Path;

/**
 * Provides configuration settings for OAuth2 credentials.
 */
public interface CredentialsSettings {

    /**
     * Directory containing the credential files rotated by mint.
     */
    Path getCredentialsDirectory();
}
