package org.zalando.undertaking.oauth2;

import com.google.inject.AbstractModule;

/**
 * Provides OAuth2 support for incoming HTTP requests as well as for obtaining access tokens for outbound requests.
 *
 * @deprecated  Use {@link AccessTokensModule} and {@link AuthenticationInfoModule} directly. This grants more
 *              flexibility as the access token is typically required in more places then just the components handling
 *              incoming HTTP requests. This class is scheduled for removal in Undertaking 0.1.0.
 */
@Deprecated
public class OAuth2Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new AccessTokensModule());
        install(new AuthenticationInfoModule());
    }
}
