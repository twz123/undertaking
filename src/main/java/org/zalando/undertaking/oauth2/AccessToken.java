package org.zalando.undertaking.oauth2;

import static java.nio.charset.StandardCharsets.UTF_8;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

public final class AccessToken {

    private static final Splitter SPLITTER = Splitter.on(' ').omitEmptyStrings().limit(2);

    private static final Optional<String> BEARER = Optional.of("Bearer");

    private final Optional<String> type;
    private final String value;

    private AccessToken(final Optional<String> type, final String value) {
        this.type = requireNonNull(type);
        this.value = value;
    }

    public static AccessToken parse(final String value) {
        final List<String> parts = SPLITTER.splitToList(value);

        switch (parts.size()) {

            default :
                return AccessToken.of(parts.get(0), parts.get(1));

            case 1 :
                return new AccessToken(Optional.empty(), parts.get(0));

            case 0 :
                return new AccessToken(Optional.empty(), null);
        }
    }

    /**
     * @deprecated  Use {@code #bearer(String)} instead.
     */
    @Deprecated
    public static AccessToken of(final String value) {
        return bearer(value);
    }

    public static AccessToken typeless(final String value) {
        return new AccessToken(Optional.empty(), value);
    }

    public static AccessToken bearer(final String value) {
        return new AccessToken(BEARER, value);
    }

    public static AccessToken of(final String type, final String value) {
        return new AccessToken(BEARER.get().equals(type) ? BEARER : Optional.of(type), value);
    }

    public boolean hasType() {
        return type.isPresent();
    }

    public boolean isOfType(final String type) {
        requireNonNull(type);
        return this.type.isPresent() && this.type.get().equals(type);
    }

    public String getValue() {
        return value;
    }

    public String getTypeAndValue() {
        final String value = Strings.nullToEmpty(this.value);
        return type.isPresent() ? type.get() + ' ' + value : value;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(64);

        buf.append("AccessToken(");

        type.ifPresent(type -> buf.append(type).append(' '));

        if (value == null) {
            buf.append("<null>");
        } else {
            buf.append("sha1:").append(Hashing.sha1().hashString(value, UTF_8));
        }

        return buf.append(')').toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AccessToken that = (AccessToken) o;
        return Objects.equal(type, that.type) && Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, value);
    }
}
