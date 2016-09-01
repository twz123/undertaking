package org.zalando.undertaking.oauth2.credentials;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import rx.Single;

public class CredentialsModule extends AbstractModule {
    @Override
    protected void configure() {
        // all in the provider methods
    }

    @Provides
    @Singleton
    Single<RequestCredentials> provideRequestCredentials(final UserCredentialsProvider userProvider,
            final ClientCredentialsProvider clientProvider) {
        return new RequestCredentialsProvider(clientProvider.get(), userProvider.get()).get();
    }
}
