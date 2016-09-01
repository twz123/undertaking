package org.zalando.undertaking.oauth2;

import org.zalando.undertaking.inject.HttpExchangeScoped;
import org.zalando.undertaking.inject.Request;

import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;

import rx.Single;

/**
 * Provides OAuth2 authentication info for HTTP requests.
 */
final class AuthenticationInfoModule extends PrivateModule {

    @Override
    protected void configure() {
        final TypeLiteral<Single<AccessToken>> accessToken = new TypeLiteral<Single<AccessToken>>() {
            // capture generic type
        };

        final TypeLiteral<Single<AuthenticationInfo>> authenticationInfo =
            new TypeLiteral<Single<AuthenticationInfo>>() {
                // capture generic type
            };

        bind(accessToken).annotatedWith(Request.class).toProvider(UndertowAuthorizationBearerTokenProvider.class).in(
            HttpExchangeScoped.class);

        bind(authenticationInfo).toProvider(AuthenticationInfoProvider.class).in(HttpExchangeScoped.class);
        expose(authenticationInfo);
    }

}
