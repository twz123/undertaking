package org.zalando.undertaking.oauth2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.common.collect.ImmutableList;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;

import rx.internal.operators.OnSubscribeSingle;

import rx.subjects.AsyncSubject;

/**
 * Provides authentication by delegating to a chain of other providers. The {@code AuthenticationInfo} of the first
 * provider emitting a successful result will be forwarded.
 */
public class AuthenticationInfoProviderChain implements Provider<Single<AuthenticationInfo>> {

    private final List<Provider<Single<AuthenticationInfo>>> providerChain;

    @Inject
    public AuthenticationInfoProviderChain(final List<Provider<Single<AuthenticationInfo>>> providerChain) {
        this.providerChain = ImmutableList.copyOf(providerChain);
    }

    private final Observable<AuthenticationInfo> missingAuth = //
        Observable.defer(() ->
                Observable.error(
                    new BadTokenInfoException("Authentication failed.",
                        "None of the authentication providers was able to authenticate the request.")));

    @Override
    public Single<AuthenticationInfo> get() {
        final Observable<AuthenticationInfo> observable = //
            Observable.concatEager(getObservableChain()).take(1).switchIfEmpty(missingAuth);
        return Single.create(new CachedSubscribe<>(observable));
    }

    private List<Observable<AuthenticationInfo>> getObservableChain() {
        final ImmutableList.Builder<Observable<AuthenticationInfo>> builder = ImmutableList.builder();
        for (final Provider<Single<AuthenticationInfo>> provider : providerChain) {
            builder.add(provider.get().toObservable() //
                .onErrorResumeNext(AuthenticationInfoProviderChain::dropBadTokenExceptions));
        }

        return builder.build();
    }

    private static <T> Observable<T> dropBadTokenExceptions(final Throwable error) {
        return error instanceof BadTokenInfoException ? Observable.empty() : Observable.error(error);
    }

    private static final class CachedSubscribe<T> implements Single.OnSubscribe<T> {

        private volatile Object state;

        CachedSubscribe(final Observable<T> source) {
            state = source;
        }

        @Override
        public void call(final SingleSubscriber<? super T> subscriber) {

            Object currentState = state;
            OnSubscribeSingle<T> cachedState;

            if (currentState instanceof OnSubscribeSingle) {
                cachedState = (OnSubscribeSingle<T>) currentState;
            } else {
                final AsyncSubject<T> subject = AsyncSubject.create();
                cachedState = OnSubscribeSingle.create(subject);

                synchronized (this) {
                    currentState = state;
                    if (currentState instanceof OnSubscribeSingle) {
                        cachedState = (OnSubscribeSingle<T>) currentState;
                        currentState = null;
                    } else {
                        state = cachedState;
                    }
                }

                if (currentState != null) {
                    ((Observable<T>) currentState).subscribe(subject);
                }
            }

            cachedState.call(subscriber);
        }
    }
}
