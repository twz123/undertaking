package org.zalando.undertaking.oauth2;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.not;

import org.junit.Test;

import com.google.common.hash.Hashing;

public class AccessTokenTest {
    public static final String TEST_TOKEN_VALUE = "token-value";
    private final String TOKEN_VALUE_SHA1 = Hashing.sha1().hashString(TEST_TOKEN_VALUE, UTF_8).toString();

    @Test
    public void testValidToken() {
        AccessToken validToken = AccessToken.parse("Bearer " + TEST_TOKEN_VALUE);

        assertThat(validToken, equalTo(AccessToken.bearer(TEST_TOKEN_VALUE)));
        assertThat(validToken.hasType(), is(true));
        assertThat(validToken.isOfType("Bearer"), is(true));
        assertThat(validToken.getTypeAndValue(), equalTo("Bearer " + TEST_TOKEN_VALUE));
        assertThat(validToken.toString(), equalTo("AccessToken(Bearer sha1:" + TOKEN_VALUE_SHA1 + ")"));
    }

    @Test
    public void testTypelessToken() {
        AccessToken typelessToken = AccessToken.parse(TEST_TOKEN_VALUE);

        assertThat(typelessToken, equalTo(AccessToken.typeless(TEST_TOKEN_VALUE)));
        assertThat(typelessToken.hasType(), is(false));
        assertThat(typelessToken.getTypeAndValue(), equalTo(TEST_TOKEN_VALUE));
        assertThat(typelessToken.toString(), equalTo("AccessToken(sha1:" + TOKEN_VALUE_SHA1 + ")"));
    }

    @Test
    public void testNullToken() {
        AccessToken nullToken = AccessToken.parse("");

        assertThat(nullToken, equalTo(AccessToken.typeless(null)));
        assertThat(nullToken.hasType(), is(false));
        assertThat(nullToken.getTypeAndValue(), equalTo(""));
        assertThat(nullToken.toString(), equalTo("AccessToken(<null>)"));
    }

    @Test
    public void testHashCodeEqualsContract() {
        AccessToken a = AccessToken.of("Bearer", "test-token");
        AccessToken b = AccessToken.of("Bearer", "test-token");
        AccessToken c = AccessToken.of("Bearer", "different-token");

        assertThat(a, equalTo(b));
        assertThat(a, not(equalTo(c)));

        assertThat(a.hashCode(), equalTo(b.hashCode()));
        assertThat(a.hashCode(), not(equalTo(c.hashCode())));
    }

}
