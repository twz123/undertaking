package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;

import static org.mockito.Mockito.when;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;

import java.net.URI;

import java.util.Collections;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import org.mockserver.client.server.MockServerClient;

import org.mockserver.junit.MockServerRule;

import org.mockserver.matchers.Times;

import com.google.common.collect.ImmutableMap;

import com.google.gson.Gson;

import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

@RunWith(MockitoJUnitRunner.class)
public class TokenInfoRequestProviderIT {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Rule
    public MockServerRule serverRule = new MockServerRule(this);

    private AsyncHttpClient httpClient;

    private MockServerClient mockServerClient;

    @Mock
    private AuthenticationInfoSettings settings;

    private TokenInfoRequestProvider underTest;

    @Before
    public void setUp() {
        httpClient = new DefaultAsyncHttpClient();

        when(settings.getTokenInfoEndpoint()).thenReturn(URI.create(
                "http://localhost:" + serverRule.getPort() + "/tokeninfo"));
        when(settings.getBusinessPartnerIdOverrideHeader()).thenReturn("X-Business-Partner-Id");

        underTest = new TokenInfoRequestProvider(settings, httpClient);
    }

    @After
    public void tearDown() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

    @Test
    public void injectsBusinessPartnerId() {
        mockServerClient.when(
                            request()                                                                              //
                            .withMethod("GET")                                                                     //
                            .withPath("/tokeninfo")                                                                //
                            .withQueryStringParameter("access_token", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),     //
                            Times.once())                                                                          //
                        .respond(
                            response()                                                                             //
                            .withStatusCode(StatusCodes.OK)                                                        //
                            .withHeader(CONTENT_TYPE, "application/json;charset=UTF-8")                            //
                            .withBody(new Gson().toJson(
                                    ImmutableMap.of(                                                               //
                                        "uid", "testuser",                                                         //
                                        "scope", Collections.emptySet()))));

        final AuthenticationInfo authInfo = underTest.createCommand(AccessToken.of(
                                                             "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
                                                         provideRequestHeaders()).toObservable().toBlocking().first();

        assertThat(authInfo, hasFeature(AuthenticationInfo::getBusinessPartnerId, hasValue("4711")));
    }

    @Test
    public void handlesBadRequest() {
        mockServerClient.when(
                            request()                                             //
                            .withMethod("GET")                                    //
                            .withPath("/tokeninfo")                               //
                            .withQueryStringParameter("access_token", "foo"),     //
                            Times.once())                                         //
                        .respond(
                            response()                                            //
                            .withStatusCode(StatusCodes.BAD_REQUEST)              //
                            .withHeader(CONTENT_TYPE, "application/json;charset=UTF-8") //
                            .withBody(new Gson().toJson(
                                    ImmutableMap.of(                              //
                                        "error", "invalid_request",               //
                                        "error_description", "Access Token not valid"))));

        expected.expectCause(allOf(                                                  //
                instanceOf(BadTokenInfoException.class),                             //
                hasProperty("error", is("invalid_token")),                           //
                hasProperty("errorDescription", hasValue("Access Token not valid"))) //
            );

        underTest.createCommand(AccessToken.of("foo"), provideRequestHeaders()).toObservable().toBlocking().first();
    }

    private static HeaderMap provideRequestHeaders() {
        final HeaderMap requestHeaders = new HeaderMap();
        requestHeaders.put(new HttpString("X-Business-Partner-Id"), "4711");

        return requestHeaders;
    }
}
