package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.hystrix.HystrixCommands;

import rx.Single;
import rx.SingleSubscriber;

import rx.subjects.AsyncSubject;

final class AuthenticationInfoProvider implements Provider<Single<AuthenticationInfo>> {

    private final Single<AccessToken> accessToken;
    private final TokenInfoRequestProvider requestProvider;

    @Inject
    AuthenticationInfoProvider(final Single<AccessToken> accessToken, final TokenInfoRequestProvider requestProvider) {
        this.accessToken = requireNonNull(accessToken);
        this.requestProvider = requireNonNull(requestProvider);
    }

    @Override
    public Single<AuthenticationInfo> get() {
        final Single<AuthenticationInfo> source = accessToken.flatMap(token -> {
                return HystrixCommands.withRetries(() -> requestProvider.createCommand(token), 3);
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
