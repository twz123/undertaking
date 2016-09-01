package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Represents a predicate (boolean-valued function), which can describe why the tested {@link AuthenticationInfo} does
 * not suffice the requirements.
 */
public interface AuthenticationInfoPredicate extends Predicate<AuthenticationInfo> {

    default Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
        return Optional.empty();
    }

    default @Override AuthenticationInfoPredicate and(final Predicate<? super AuthenticationInfo> other) {
        requireNonNull(other);

        return new AuthenticationInfoPredicate() {
            @Override
            public boolean test(final AuthenticationInfo authenticationInfo) {
                return AuthenticationInfoPredicate.this.test(authenticationInfo) && other.test(authenticationInfo);
            }

            @Override
            public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
                final List<String> descs = new ArrayList<>(2);

                if (!AuthenticationInfoPredicate.this.test(authInfo)) {
                    AuthenticationInfoPredicate.this.getErrorDescription(authInfo).ifPresent(descs::add);
                }

                if (other instanceof AuthenticationInfoPredicate && !other.test(authInfo)) {
                    ((AuthenticationInfoPredicate) other).getErrorDescription(authInfo).ifPresent(descs::add);
                }

                return descs.isEmpty() ? Optional.empty() : Optional.of(String.join(" ", descs));
            }
        };
    }

    default @Override AuthenticationInfoPredicate or(final Predicate<? super AuthenticationInfo> other) {
        requireNonNull(other);

        return new AuthenticationInfoPredicate() {
            @Override
            public boolean test(final AuthenticationInfo authenticationInfo) {
                return AuthenticationInfoPredicate.this.test(authenticationInfo) || other.test(authenticationInfo);
            }

            @Override
            public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
                if (!AuthenticationInfoPredicate.this.test(authInfo)) {
                    return AuthenticationInfoPredicate.this.getErrorDescription(authInfo);
                }

                if (other instanceof AuthenticationInfoPredicate && !other.test(authInfo)) {
                    return ((AuthenticationInfoPredicate) other).getErrorDescription(authInfo);
                }

                return Optional.empty();
            }
        };
    }
}
