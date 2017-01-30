package org.zalando.undertaking.oauth2;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zalando.undertaking.oauth2.credentials.CredentialsModule;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import com.google.inject.AbstractModule;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;

import io.reactivex.Single;

/**
 * Provides the access token for accessing OAuth secured services.
 */
public class AccessTokensModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokensModule.class);

    private static final TypeLiteral<Single<AccessToken>> TOKEN_TYPE = new TypeLiteral<Single<AccessToken>>() {
        // capture generic type
    };

    private static Iterable<Map.Entry<String, AccessToken>> deserializeTokens(final String tokens) {
        final Splitter tokenSplitter = Splitter.on('=').limit(2);
        return FluentIterable.from(Splitter.on(',').split(tokens)).transform(token -> {
                final Iterator<String> splitter = tokenSplitter.split(token).iterator();
                final String name = CharMatcher.whitespace().trimFrom(Iterators.getNext(splitter, ""));
                final String value = CharMatcher.whitespace().trimFrom(Iterators.getNext(splitter, ""));
                return Maps.immutableEntry(name, AccessToken.bearer(value));
            });
    }

    @Override
    protected void configure() {
        final Optional<AccessToken> fixedToken = obtainFixedToken();
        if (fixedToken.isPresent()) {
            LOG.info("Using fixed acccess token");
            bind(TOKEN_TYPE).toInstance(Single.just(fixedToken.get()));
        } else {
            install(new TokenRefresherModule());
        }
    }

    private Optional<AccessToken> obtainFixedToken() {
        return
            Optional.ofNullable(getAccessTokensStringFromEnvironment()) //
                    .map(AccessTokensModule::deserializeTokens)         //
                    .map(tokens -> Iterables.getFirst(tokens, null))    //
                    .map(Map.Entry::getValue);
    }

    @VisibleForTesting
    @SuppressWarnings("static-method")
    String getAccessTokensStringFromEnvironment() {
        return System.getenv("OAUTH2_ACCESS_TOKENS");
    }

    @VisibleForTesting
    @SuppressWarnings("static-method")
    void startAutoUpdate(final AccessTokenProvider accessTokenProvider) {

        // start auto-updater
        // currently, we don't care about unsubscription, since we don't have any facility to "shutdown" the app
        accessTokenProvider.autoUpdate();
    }

    private final class TokenRefresherModule extends PrivateModule {

        @Override
        protected void configure() {
            install(new CredentialsModule());

            enableAccessTokensAutoUpdate();

            bind(TOKEN_TYPE).toProvider(AccessTokenProvider.class).in(Singleton.class);
            expose(TOKEN_TYPE);
        }

        private void enableAccessTokensAutoUpdate() {
            binder().bindListener(Matchers.any(), new ProvisionListener() {
                    @Override
                    public <T> void onProvision(final ProvisionInvocation<T> provision) {
                        if (provision.getBinding().getKey().getTypeLiteral().getRawType()
                                == AccessTokenProvider.class) {
                            startAutoUpdate((AccessTokenProvider) provision.provision());
                        }
                    }
                });
        }
    }
}
