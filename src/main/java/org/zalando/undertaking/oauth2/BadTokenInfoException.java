package org.zalando.undertaking.oauth2;

import java.util.Optional;

import com.google.common.base.MoreObjects;

/**
 * Thrown to indicate a negative outcome of an access token info request.
 */
@SuppressWarnings("serial")
public final class BadTokenInfoException extends RuntimeException {

    private String error;
    private Optional<String> errorDescription;

    public BadTokenInfoException(final String error, final String errorDescription) {
        this.error = MoreObjects.firstNonNull(error, "unknown");
        this.errorDescription = Optional.ofNullable(errorDescription);
    }

    public BadTokenInfoException(final Throwable cause) {
        super(cause);
    }

    public BadTokenInfoException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public String getError() {
        return error;
    }

    public Optional<String> getErrorDescription() {
        return errorDescription;
    }

    @Override
    public String getMessage() {
        return errorDescription.map(desc -> (error + ": " + desc)).orElse(error);
    }
}
