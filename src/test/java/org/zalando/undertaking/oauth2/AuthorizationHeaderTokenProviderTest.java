package org.zalando.undertaking.oauth2;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class AuthorizationHeaderTokenProviderTest {
    @Test
    public void extractsTokenHeader() {
        HeaderMap headerMapWithAuthorization = new HeaderMap();
        headerMapWithAuthorization.put(Headers.AUTHORIZATION, "Bearer test-token");

        new AuthorizationHeaderTokenProvider(headerMapWithAuthorization)
            .get()
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(AccessToken.of("Bearer", "test-token"));
    }

    @Test
    public void extractsTokenHeaderNoToken() {
        HeaderMap emptyHeaderMap = new HeaderMap();

        new AuthorizationHeaderTokenProvider(emptyHeaderMap)                             //
            .get()                                                                       //
            .test()                                                                      //
            .awaitDone(10, TimeUnit.SECONDS).assertError(NoAccessTokenException.class);  //
    }

}
