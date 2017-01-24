package org.zalando.undertaking.oauth2;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.reactivestreams.Publisher;

import com.google.common.collect.ImmutableList;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import io.reactivex.functions.Function;

import io.reactivex.subjects.AsyncSubject;

/**
 * Provides authentication by delegating to a chain of other providers. The {@code AuthenticationInfo} of the first
 * provider emitting a successful result will be forwarded.
 */
public class AuthenticationInfoProviderChain implements Provider<Single<AuthenticationInfo>> {

    private final List<Provider<Single<AuthenticationInfo>>> providerChain;
    private final Flowable<AuthenticationInfo> missingAuth = //
        Flowable.defer(() ->
                Flowable.error(
                    new BadTokenInfoException("invalid_request",
                        "None of the authentication providers was able to authenticate the request.")));

    @Inject
    public AuthenticationInfoProviderChain(final List<Provider<Single<AuthenticationInfo>>> providerChain) {
        this.providerChain = ImmutableList.copyOf(providerChain);
    }

    private static <T> Flowable<T> dropBadOrMalformedTokenExceptions(final Throwable error) {
        return (error instanceof BadTokenInfoException || error instanceof MalformedAccessTokenException)
            ? Flowable.empty() : Flowable.error(error);
    }

    @Override
    public Single<AuthenticationInfo> get() {
        final Flowable<AuthenticationInfo> observable = //
            Flowable.concatEager(getObservableChain()).take(1).switchIfEmpty(missingAuth);

        return Single.create(new CachedSubscribe<>(observable));
    }

    private List<Flowable<AuthenticationInfo>> getObservableChain() {
        final ImmutableList.Builder<Flowable<AuthenticationInfo>> builder = ImmutableList.builder();
        for (final Provider<Single<AuthenticationInfo>> provider : providerChain) {
            builder.add(provider.get().toFlowable() //
                .onErrorResumeNext(
                    (Function<? super Throwable, ? extends Publisher<? extends AuthenticationInfo>>)
                    AuthenticationInfoProviderChain::dropBadOrMalformedTokenExceptions));
        }

        return builder.build();
    }

    private static final class CachedSubscribe<T> extends AtomicReference<Single<T>> implements SingleOnSubscribe<T> {

        private final Flowable<? extends T> source;

        CachedSubscribe(final Flowable<? extends T> source) {
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
                    source.subscribe(subject::onNext, subject::onError, subject::onComplete);
                    break;
                }
            }

            single.subscribe(emitter::onSuccess, emitter::onError);
        }
    }
}
