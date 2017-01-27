package org.zalando.undertaking.oauth2;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class AuthorizationHeaderTokenProviderTest {
    @Test
    public void extractsTokenHeader() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.AUTHORIZATION, "Bearer test-token");

        new AuthorizationHeaderTokenProvider(headerMap).get()                           //
                                                       .test()                          //
                                                       .awaitDone(10, TimeUnit.SECONDS) //
                                                       .assertValue(AccessToken.of("Bearer", "test-token"));
    }

    @Test
    public void extractsTokenHeaderNoToken() {
        HeaderMap headerMap = new HeaderMap();

        new AuthorizationHeaderTokenProvider(headerMap).get()                           //
                                                       .test()                          //
                                                       .awaitDone(10, TimeUnit.SECONDS) //
                                                       .assertError(NoAccessTokenException.class);
    }

}
