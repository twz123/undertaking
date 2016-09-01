package org.zalando.undertaking.oauth2;

/**
 * Indicates a problem while communicating with the access token endpoint.
 */
public class AccessTokenRequestException extends RuntimeException {
    public AccessTokenRequestException(final String message) {
        super(message);
    }

    public AccessTokenRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
