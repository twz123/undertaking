package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.hystrix.HystrixCommands;
import org.zalando.undertaking.inject.Request;

import io.undertow.util.HeaderMap;

import rx.Single;
import rx.SingleSubscriber;

import rx.subjects.AsyncSubject;

public final class AuthenticationInfoProvider implements Provider<Single<AuthenticationInfo>> {

    private static final Single<AuthenticationInfo> MALFORMED_TOKEN = Single.error(new MalformedAccessTokenException());

    private final Provider<Single<AccessToken>> accessTokenProvider;
    private final Provider<HeaderMap> requestHeadersProvider;
    private final TokenInfoRequestProvider requestProvider;

    @Inject
    AuthenticationInfoProvider(@Request final Provider<Single<AccessToken>> accessTokenProvider,
            @Request final Provider<HeaderMap> requestHeadersProvider, final TokenInfoRequestProvider requestProvider) {
        this.accessTokenProvider = requireNonNull(accessTokenProvider);
        this.requestHeadersProvider = requireNonNull(requestHeadersProvider);
        this.requestProvider = requireNonNull(requestProvider);
    }

    @Override
    public Single<AuthenticationInfo> get() {
        final HeaderMap requestHeaders = requestHeadersProvider.get();
        final Single<AuthenticationInfo> source = accessTokenProvider.get().flatMap(token -> {

                if (!token.isOfType("Bearer")) {
                    return MALFORMED_TOKEN;
                }

                return HystrixCommands.withRetries(() -> requestProvider.createCommand(token, requestHeaders), 3)
                                      .toSingle();
            });

        return Single.create(new CachedSubscribe<>(source));
    }

    @SuppressWarnings("serial")
    private static final class CachedSubscribe<T> extends AtomicReference<Single<T>> implements Single.OnSubscribe<T> {

        private final Single<? extends T> source;

        CachedSubscribe(final Single<? extends T> source) {
            this.source = source;
        }

        @Override
        public void call(final SingleSubscriber<? super T> subscriber) {

            Single<T> emitter;

            for (;;) {
                emitter = get();
                if (emitter != null) {
                    break;
                }

                final AsyncSubject<T> subject = AsyncSubject.create();
                emitter = subject.toSingle();
                if (compareAndSet(null, emitter)) {
                    source.subscribe(subject);
                    break;
                }
            }

            emitter.subscribe(subscriber);
        }
    }
}
