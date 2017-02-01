package org.zalando.undertaking.oauth2;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Single;

import io.undertow.util.HeaderMap;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationInfoProviderTest {

    private final AccessToken accessToken = AccessToken.bearer("token");
    private final HeaderMap requestHeaders = new HeaderMap();
    @Mock
    private Provider<Single<AccessToken>> accessTokenProvider;
    @Mock
    private Provider<HeaderMap> requestHeadersProvider;
    @Mock
    private TokenInfoRequestProvider requestProvider;
    @Mock
    private AuthenticationInfo authenticationInfo;
    private AuthenticationInfoProvider underTest;

    private static <T> Single<T> mockSuccess(final T result) {
        return Single.just(result);
    }

    @Before
    public void initializeTest() {
        when(accessTokenProvider.get()).thenReturn(Single.just(accessToken));
        when(requestHeadersProvider.get()).thenReturn(requestHeaders);

        underTest = new AuthenticationInfoProvider(accessTokenProvider, requestHeadersProvider, requestProvider);
    }

    @Test
    public void callsEndpointOnlyOnce() throws InterruptedException {
        doReturn(mockSuccess(authenticationInfo)).when(requestProvider).getTokenInfo(any(), any());

        Single<AuthenticationInfo> single = underTest.get();
        single.test().awaitDone(1, TimeUnit.SECONDS).assertValue(authenticationInfo);

        verify(requestProvider).getTokenInfo(accessToken, requestHeaders);
        verifyNoMoreInteractions(requestProvider);

        single.test().awaitDone(1, TimeUnit.SECONDS).assertValue(authenticationInfo);
    }

}
