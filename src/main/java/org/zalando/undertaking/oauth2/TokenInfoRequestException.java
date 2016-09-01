package org.zalando.undertaking.oauth2;

/**
 * Indicates a problem while communicating with a token info endpoint.
 */
public class TokenInfoRequestException extends RuntimeException {

    public TokenInfoRequestException(final String message) {
        super(message);
    }

    public TokenInfoRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
