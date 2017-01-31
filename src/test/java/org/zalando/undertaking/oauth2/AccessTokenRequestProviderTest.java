package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import static org.hobsoft.hamcrest.compose.ComposeMatchers.compose;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeatureValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import java.time.Clock;
import java.time.Instant;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Param;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import org.zalando.undertaking.ahc.GuardedHttpClient;
import org.zalando.undertaking.oauth2.credentials.ClientCredentials;
import org.zalando.undertaking.oauth2.credentials.RequestCredentials;
import org.zalando.undertaking.oauth2.credentials.UserCredentials;

import com.google.common.collect.ImmutableSet;

import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;

import io.reactivex.Single;

import io.reactivex.observers.TestObserver;

import io.reactivex.plugins.RxJavaPlugins;

import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokenRequestProviderTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Mock
    private AccessTokenSettings settings;

    @Mock
    private AsyncHttpClient client;

    private RequestCredentials credentials;

    @Mock
    private Clock clock;

    @Mock
    private BoundRequestBuilder requestBuilder;

    @Mock
    private Response response;

    private Single<Response> requestSingle;

    private AccessTokenRequestProvider underTest;

    @Captor
    private ArgumentCaptor<List<Param>> formParamCaptor;

    @Before
    public void initializeTest() {
        GuardedHttpClient guardedHttpClient = new GuardedHttpClient(CircuitBreakerRegistry.ofDefaults(),
                (e) -> requestSingle);

        this.underTest = new AccessTokenRequestProvider(settings, client, clock, guardedHttpClient);

        credentials = new RequestCredentials(new ClientCredentials("clientId", "clientSecret"),
                new UserCredentials("user", "pass"));

        when(settings.getAccessTokenEndpoint()).thenReturn(URI.create("foo"));
        when(client.preparePost(any())).thenReturn(requestBuilder);

        when(requestBuilder.setRealm(any(Realm.class))).thenReturn(requestBuilder);
        when(requestBuilder.setHeader(any(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.addQueryParam(any(), any())).thenReturn(requestBuilder);
        when(requestBuilder.setFormParams(ArgumentMatchers.<List<Param>>any())).thenReturn(requestBuilder);
    }

    @After
    public void tearDown() {
        RxJavaPlugins.reset();
    }

    @Test
    public void extractsToken() {
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn("{access_token:foo, expires_in:5}");
        when(clock.instant()).thenReturn(Instant.ofEpochSecond(0), Instant.ofEpochSecond(60));
        requestSingle = Single.just(response);

        final Single<AccessTokenResponse> requestSingle = underTest.requestAccessToken(credentials);

        final AccessTokenResponse first = requestSingle.blockingGet();
        assertThat(first.getAccessToken(), hasProperty("value", is("foo")));
        assertThat(first.getExpiryTime(), is(Instant.ofEpochSecond(5)));

        final AccessTokenResponse second = requestSingle.blockingGet();
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
    public void forwardsErrors() throws InterruptedException {
        final Exception error = new Exception("foo");
        requestSingle = Single.error(error);

        underTest.requestAccessToken(credentials).test().await().assertError(error);
    }

    @Test
    public void timeout() throws Exception {
        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler((s) -> testScheduler);

        requestSingle = Single.never();

        TestObserver<AccessTokenResponse> testObserver = underTest.requestAccessToken(credentials).test();

        // Timeout is expected to be set < 1 min
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        testObserver.assertError(TimeoutException.class);
    }

    @Test
    public void usesSpaceAsScopeSeparator() throws InterruptedException {
        requestSingle = Single.just(response);
        when(settings.getAccessTokenScopes()).thenReturn(ImmutableSet.of("scope1", "scope2"));

        underTest.requestAccessToken(credentials).test().await();

        verify(requestBuilder).setFormParams(formParamCaptor.capture());

        assertThat(formParamCaptor.getValue(),
            hasItem(
                compose("a scope form parameter", hasFeatureValue("with name", Param::getName, "scope")).and(
                    hasFeatureValue("with value", Param::getValue, "scope1 scope2"))));
    }

    private AccessTokenResponse requestToken() {
        return underTest.requestAccessToken(credentials).blockingGet();
    }
}
