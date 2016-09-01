package org.zalando.undertaking.oauth2;

import com.google.inject.AbstractModule;

/**
 * Provides OAuth2 support for incoming HTTP requests as well as for obtaining access tokens for outbound requests.
 */
public class OAuth2Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new AccessTokensModule());
        install(new AuthenticationInfoModule());
    }
}
