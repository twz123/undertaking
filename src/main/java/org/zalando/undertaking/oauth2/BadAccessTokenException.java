package org.zalando.undertaking.oauth2;

import java.util.Optional;

import com.google.common.base.MoreObjects;

/**
 * Thrown to indicate a negative outcome of an access token creation request.
 */
public final class BadAccessTokenException extends RuntimeException {

    private final Optional<String> errorDescription;

    BadAccessTokenException(final String error, final String errorDescription, final Throwable cause) {
        super(MoreObjects.firstNonNull(error, "unknown"), cause);
        this.errorDescription = Optional.ofNullable(errorDescription);
    }

    BadAccessTokenException(final String error, final String errorDescription) {
        this(error, errorDescription, null);
    }

    BadAccessTokenException(final Throwable cause) {
        this(null, cause);
    }

    BadAccessTokenException(final String error, final Throwable cause) {
        this(error, null, cause);
    }

    public String getError() {
        return super.getMessage();
    }

    public Optional<String> getErrorDescription() {
        return errorDescription;
    }

    @Override
    public String getMessage() {
        return errorDescription.map(desc -> (getError() + ": " + desc)).orElse(getError());
    }
}
