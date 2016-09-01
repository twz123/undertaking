package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.TIMEOUT;

import java.lang.reflect.Method;

import java.net.URI;

import java.time.Clock;
import java.time.Instant;

import java.util.List;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Param;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.junit.runner.RunWith;

import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import org.mockito.runners.MockitoJUnitRunner;

import org.zalando.undertaking.oauth2.credentials.RequestCredentials;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import rx.Observable;
import rx.Single;

import rx.observers.TestSubscriber;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokenRequestProviderTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Mock
    private OAuth2Settings settings;

    @Mock
    private AsyncHttpClient client;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RequestCredentials credentials;

    @Mock
    private Clock clock;

    @Spy
    @InjectMocks
    private AccessTokenRequestProvider underTest;

    @Mock
    private BoundRequestBuilder requestBuilder;

    private Single<Response> requestSingle;

    @Mock
    private Response response;

    @Before
    public void initializeTest() {
        when(settings.getAccessTokenEndpoint()).thenReturn(URI.create("foo"));
        when(credentials.getClientCredentials().getClientId()).thenReturn("clientId");
        when(credentials.getClientCredentials().getClientSecret()).thenReturn("clientSecret");
        when(credentials.getUserCredentials().getApplicationUsername()).thenReturn("user");
        when(credentials.getUserCredentials().getApplicationPassword()).thenReturn("pass");
        when(client.preparePost(any())).thenReturn(requestBuilder);
        doReturn(Single.defer(() -> requestSingle)).when(underTest).createRequest(any());

        when(requestBuilder.setRealm(any(Realm.class))).thenReturn(requestBuilder);
        when(requestBuilder.setHeader(any(), any())).thenReturn(requestBuilder);
        when(requestBuilder.addQueryParam(any(), any())).thenReturn(requestBuilder);
        when(requestBuilder.setFormParams(ArgumentMatchers.<List<Param>>any())).thenReturn(requestBuilder);
    }

    @Test
    public void fullCoverage4TW() {
        doCallRealMethod().when(underTest).createRequest(any());
        underTest.createRequest(requestBuilder);
    }

    @Test
    public void extractsToken() {
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn("{access_token:foo, expires_in:5}");
        when(clock.instant()).thenReturn(Instant.ofEpochSecond(0), Instant.ofEpochSecond(60));
        requestSingle = Single.just(response);

        final Single<AccessTokenResponse> requestSingle = underTest.requestAccessToken(credentials);

        final AccessTokenResponse first = requestSingle.toBlocking().value();
        verify(underTest).createRequest(any());
        assertThat(first.getAccessToken(), hasProperty("value", is("foo")));
        assertThat(first.getExpiryTime(), is(Instant.ofEpochSecond(5)));

        final AccessTokenResponse second = requestSingle.toBlocking().value();
        verify(underTest).createRequest(any());
        assertThat(second.getAccessToken(), hasProperty("value", is("foo")));
        assertThat(second.getExpiryTime(), is(Instant.ofEpochSecond(65)));
    }

    @Test
    public void handlesClientError() {
        when(response.getStatusCode()).thenReturn(400);
        when(response.getResponseBody()).thenReturn("{error: foo, error_description: bar}");
        requestSingle = Single.just(response);

        expected.expect(BadAccessTokenException.class);
        expected.expectMessage("foo: bar");

        requestToken();
    }

    @Test
    public void handlesClientErrorWithEmptyPayload() {
        when(response.getStatusCode()).thenReturn(400);
        when(response.getResponseBody()).thenReturn("");
        requestSingle = Single.just(response);

        expected.expect(BadAccessTokenException.class);
        expected.expectMessage("unknown");

        requestToken();
    }

    @Test
    public void handlesClientErrorWithEmptyJson() {
        when(response.getStatusCode()).thenReturn(400);
        when(response.getResponseBody()).thenReturn("{}");
        requestSingle = Single.just(response);

        expected.expect(BadAccessTokenException.class);
        expected.expectMessage("unknown");

        requestToken();
    }

    @Test
    public void handlesClientErrorWithInvalidJson() {
        when(response.getStatusCode()).thenReturn(400);
        when(response.getResponseBody()).thenReturn("{error}");
        requestSingle = Single.just(response);

        expected.expect(BadAccessTokenException.class);
        expected.expectMessage("HTTP status code 400");

        requestToken();
    }

    @Test
    public void failsOnUnexpectedStatusCode() {
        when(response.getStatusCode()).thenReturn(999);
        requestSingle = Single.just(response);

        expected.expect(AccessTokenRequestException.class);
        expected.expectMessage("Unexpected status code: 999: null");

        requestToken();
    }

    @Test
    public void failsOnMalformedJson() {
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn("{access_token}");
        requestSingle = Single.just(response);

        expected.expect(AccessTokenRequestException.class);
        expected.expectMessage("Failed to parse JSON payload");

        requestToken();
    }

    @Test
    public void failsOnMissingAccessToken() {
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn("{}");
        requestSingle = Single.just(response);

        expected.expect(AccessTokenRequestException.class);
        expected.expectMessage("No access_token in response");

        requestToken();
    }

    @Test
    public void failsOnEmptyPayload() {
        when(response.getStatusCode()).thenReturn(200);
        requestSingle = Single.just(response);

        expected.expect(AccessTokenRequestException.class);
        expected.expectMessage("No payload for OK response");

        requestToken();
    }

    @Test
    public void forwardsErrors() {
        final Exception error = new Exception("foo");
        requestSingle = Single.error(error);

        final TestSubscriber<AccessTokenResponse> subscriber = new TestSubscriber<>();
        underTest.requestAccessToken(credentials).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertError(error);
    }

    @Test
    public void hystrixTimeout() {
        requestSingle = Observable.<Response>never().toSingle();

        expected.expect(HystrixRuntimeException.class);
        expected.expect(hasProperty("failureType", is(TIMEOUT)));

        requestToken();
    }

    @Test
    public void doesntUnwrapNonHystrixExceptions() throws Exception {
        final Method method = AccessTokenRequestProvider.class.getDeclaredMethod( //
                "unwrapHystrixException", Throwable.class);
        method.setAccessible(true);

        final RuntimeException error = new RuntimeException("Oh no!");
        assertThat(method.invoke(null, error), is(sameInstance(error)));
    }

    private AccessTokenResponse requestToken() {
        return underTest.requestAccessToken(credentials).toBlocking().value();
    }
}
