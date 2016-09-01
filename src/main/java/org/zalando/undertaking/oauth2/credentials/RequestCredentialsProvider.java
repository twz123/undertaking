package org.zalando.undertaking.oauth2.credentials;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;
import javax.inject.Provider;

import rx.Single;

/**
 * Provides a combination of {@code ClientCredentials} and {@code UserCredentials}.
 */
class RequestCredentialsProvider implements Provider<Single<RequestCredentials>> {

    private Single<ClientCredentials> clientCredentials;

    private Single<UserCredentials> userCredentials;

    @Inject
    public RequestCredentialsProvider(final Single<ClientCredentials> clientCredentials,
            final Single<UserCredentials> userCredentials) {
        this.clientCredentials = requireNonNull(clientCredentials);
        this.userCredentials = requireNonNull(userCredentials);
    }

    @Override
    public Single<RequestCredentials> get() {
        return Single.zip(clientCredentials, userCredentials, RequestCredentials::new);
    }
}
