package org.zalando.undertaking.oauth2;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;

import java.time.Clock;

import javax.inject.Inject;

import org.asynchttpclient.AsyncHttpClient;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Spy;

import org.mockito.runners.MockitoJUnitRunner;

import org.zalando.undertaking.ahc.GuardedHttpClient;
import org.zalando.undertaking.oauth2.credentials.CredentialsSettings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;

import io.reactivex.Single;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokensModuleTest {

    @Mock
    private CredentialsSettings credentialsSettings;

    @Mock
    private AccessTokenSettings accessTokenSettings;

    @Mock
    private AsyncHttpClient asyncHttpClient;

    @Mock
    private GuardedHttpClient guardedHttpClient;

    @Spy
    private AccessTokensModule underTest;

    @Before
    public void initializeTest() {

        // avoid NPE, otherwise unused
        when(credentialsSettings.getCredentialsDirectory()).thenReturn(Paths.get(""));

        // disable auto update for these tests
        doNothing().when(underTest).startAutoUpdate(any());
    }

    @Test
    public void fullTestCoverage4TW() {
        final AccessTokenProvider mock = mock(AccessTokenProvider.class);
        doCallRealMethod().when(underTest).startAutoUpdate(mock);
        underTest.startAutoUpdate(mock);
        underTest.getAccessTokensStringFromEnvironment();
    }

    @Test
    public void usesFixedAccessTokens() {
        doReturn("a=b=c,d=e").when(underTest).getAccessTokensStringFromEnvironment();

        final Injector injector = Guice.createInjector(underTest);
        final TokenCapture first = injector.getInstance(TokenCapture.class);
        assertThat(first.captured.blockingGet().getValue(), is("b=c"));

        final TokenCapture second = injector.getInstance(TokenCapture.class);
        assertThat(second, is(not(sameInstance(first))));
        assertThat(second.captured, is(sameInstance(first.captured)));

        verify(underTest, never()).startAutoUpdate(any());
    }

    @Test
    public void usesNonSingletonTokensWithoutFixedTokens() {
        doReturn(null).when(underTest).getAccessTokensStringFromEnvironment();

        final Injector injector = createInjector();

        final TokenCapture first = injector.getInstance(TokenCapture.class);
        final TokenCapture second = injector.getInstance(TokenCapture.class);
        assertThat(second, is(not(sameInstance(first))));

        // ensure that there is never a second AccessTokenProvider bound and started
        verify(underTest, times(1)).startAutoUpdate(any());
    }

    private Injector createInjector() {
        return Guice.createInjector(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Clock.class).toInstance(Clock.systemUTC());
                        bind(CredentialsSettings.class).toInstance(credentialsSettings);
                        bind(AccessTokenSettings.class).toInstance(accessTokenSettings);
                        bind(AsyncHttpClient.class).toInstance(asyncHttpClient);
                        bind(CircuitBreakerRegistry.class).toInstance(CircuitBreakerRegistry.ofDefaults());
                        bind(GuardedHttpClient.class).toInstance(guardedHttpClient);
                    }
                }, underTest);
    }

    static final class TokenCapture {
        final Single<AccessToken> captured;

        @Inject
        public TokenCapture(final Single<AccessToken> captured) {
            this.captured = captured;
        }
    }
}
