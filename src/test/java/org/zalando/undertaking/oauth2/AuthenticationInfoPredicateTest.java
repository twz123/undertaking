package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationInfoPredicateTest {

    @Mock
    private AuthenticationInfo authInfo;

    private Predicate<AuthenticationInfo> falsePredicate = authenticationInfo -> false;
    private Predicate<AuthenticationInfo> truePredicate = authenticationInfo -> true;

    @Test
    public void combinedErrorMessageOnFalseAIPredicateAndFalseAIPredicate() {
        assertThat(new FalseAuthenticationInfoPredicate().and(new MoreFalseAuthenticationInfoPredicate())
                .getErrorDescription(authInfo), hasValue("I'm always false. I'm always more false than others."));
    }

    @Test
    public void onlyAIErrorMessageOnFalseAIPredicateAndFalsePredicate() {
        assertThat(new FalseAuthenticationInfoPredicate().and(falsePredicate).getErrorDescription(authInfo),
            hasValue("I'm always false."));
    }

    @Test
    public void noErrorMessageOnTrueAIPredicateAndTrueAIPredicate() {
        assertThat(new TrueAuthenticationInfoPredicate().and(new TrueAuthenticationInfoPredicate()).getErrorDescription(
                authInfo), isEmpty());
    }

    @Test
    public void noErrorMessageOnTrueAIPredicateAndTruePredicate() {
        assertThat(new TrueAuthenticationInfoPredicate().and(truePredicate).getErrorDescription(authInfo), isEmpty());
    }

    @Test
    public void firstAIPredicateErrorMessageOnFalseAIPredicateOrFalseAIPredicate() {
        assertThat(new FalseAuthenticationInfoPredicate().or(new MoreFalseAuthenticationInfoPredicate())
                .getErrorDescription(authInfo), hasValue("I'm always false."));
    }

    @Test
    public void failingAIPredicateErrorMessageOnTrueAIPredicateOrFalseAIPredicate() {
        assertThat(new TrueAuthenticationInfoPredicate().or(new MoreFalseAuthenticationInfoPredicate())
                .getErrorDescription(authInfo), hasValue("I'm always more false than others."));
    }

    @Test
    public void noErrorMessageOnTrueAIPredicateOrFalsePredicate() {
        assertThat(new TrueAuthenticationInfoPredicate().or(falsePredicate).getErrorDescription(authInfo), isEmpty());
    }

    @Test
    public void noErrorMessagesOnTrueAIPredicateOrTrueAIPredicate() {
        assertThat(new TrueAuthenticationInfoPredicate().or(new TrueAuthenticationInfoPredicate()).getErrorDescription(
                authInfo), isEmpty());
    }

    private static class FalseAuthenticationInfoPredicate implements AuthenticationInfoPredicate {
        @Override
        public boolean test(final AuthenticationInfo authenticationInfo) {
            return false;
        }

        @Override
        public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
            return Optional.of("I'm always false.");
        }
    }

    private static class MoreFalseAuthenticationInfoPredicate implements AuthenticationInfoPredicate {
        @Override
        public boolean test(final AuthenticationInfo authenticationInfo) {
            return false;
        }

        @Override
        public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
            return Optional.of("I'm always more false than others.");
        }
    }

    private static class TrueAuthenticationInfoPredicate implements AuthenticationInfoPredicate {
        @Override
        public boolean test(final AuthenticationInfo authenticationInfo) {
            return true;
        }

        @Override
        public Optional<String> getErrorDescription(final AuthenticationInfo authInfo) {
            return Optional.of("I will never fail you.");
        }
    }

}
