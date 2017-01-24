package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.zalando.undertaking.inject.Request;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;

import io.reactivex.disposables.Disposable;

import io.reactivex.subjects.AsyncSubject;

import io.undertow.util.HeaderMap;

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
        final Single<AuthenticationInfo> source = accessTokenProvider.get().flatMap(token ->
                    requestProvider.getTokenInfo(token, requestHeaders));

        return Single.create(new CachedSubscribe<>(source));
    }

    @SuppressWarnings("serial")
    private static final class CachedSubscribe<T> extends AtomicReference<Single<T>> implements SingleOnSubscribe<T> {

        private final Single<? extends T> source;

        CachedSubscribe(final Single<? extends T> source) {
            this.source = source;
        }

        @Override
        public void subscribe(final SingleEmitter<T> emitter) throws Exception {
            Single<T> single;

            for (;;) {
                single = get();
                if (single != null) {
                    break;
                }

                final AsyncSubject<T> subject = AsyncSubject.create();
                single = subject.singleOrError();

                if (compareAndSet(null, single)) {
                    source.subscribe(new SingleObserver<T>() {
                            @Override
                            public void onSubscribe(final Disposable d) {
                                subject.onSubscribe(d);
                            }

                            @Override
                            public void onSuccess(final T t) {
                                subject.onNext(t);
                                subject.onComplete();
                            }

                            @Override
                            public void onError(final Throwable e) {
                                subject.onError(e);
                            }
                        });
                    break;
                }
            }

            single.subscribe(emitter::onSuccess, emitter::onError);
        }
    }
}
