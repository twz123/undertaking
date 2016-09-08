package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;

import static org.mockito.Mockito.when;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;

import java.net.URI;

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
import com.google.common.collect.ImmutableSet;

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
        when(settings.getBusinessPartnerIdOverrideScope()).thenReturn("business_partner_override");

        underTest = new TokenInfoRequestProvider(settings, httpClient, provideRequestHeaders());
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
                                        "scope", ImmutableSet.of("uid", "business_partner_override")))));

        final AuthenticationInfo authInfo = underTest.toObservable(AccessToken.of(
                    "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")).toBlocking().first();

        assertThat(authInfo, hasFeature(AuthenticationInfo::getBusinessPartnerId, hasValue("4711")));
    }

    private static HeaderMap provideRequestHeaders() {
        final HeaderMap requestHeaders = new HeaderMap();
        requestHeaders.put(new HttpString("X-Business-Partner-Id"), "4711");

        return requestHeaders;
    }
}
