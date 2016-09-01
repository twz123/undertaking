package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.MoreObjects.firstNonNull;

import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.COMMAND_EXCEPTION;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;

import javax.inject.Inject;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Param;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;

import org.asynchttpclient.extras.rxjava.single.AsyncHttpSingle;

import org.zalando.undertaking.oauth2.credentials.ClientCredentials;
import org.zalando.undertaking.oauth2.credentials.RequestCredentials;
import org.zalando.undertaking.oauth2.credentials.UserCredentials;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import io.undertow.util.StatusCodes;

import rx.Observable;
import rx.Single;

class AccessTokenRequestProvider extends RequestProvider {

    private static final class Payload {
        long expiresIn;
        String tokenType;
        String realm;
        String scope;
        String grantType;
        String uid;
        String accessToken;
    }

    private final HystrixObservableCommand.Setter hystrixSetter =
        HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("auth")) //
                                       .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                               .withExecutionTimeoutInMilliseconds(10000))         //
                                       .andCommandKey(HystrixCommandKey.Factory.asKey("accessToken"));

    private final Clock clock;

    @Inject
    public AccessTokenRequestProvider(final OAuth2Settings settings, final AsyncHttpClient client, final Clock clock) {
        super(client, settings);
        this.clock = requireNonNull(clock);
    }

    private BoundRequestBuilder createRequestBuilder(final RequestCredentials credentials) {
        return
            client.preparePost(settings.getAccessTokenEndpoint().toString()) //
                  .setRealm(createRealm(credentials.getClientCredentials())) //
                  .setHeader(HttpHeaders.ACCEPT, "application/json")         //
                  .addQueryParam("realm", "/services")                       //
                  .setFormParams(createFormParams(credentials.getUserCredentials()));
    }

    public Single<AccessTokenResponse> requestAccessToken(final RequestCredentials credentials) {
        final Single<Instant> requestTimeProvider = Single.fromCallable(clock::instant);

        final Observable<AccessTokenResponse> requestObservable =                                              //
            createRequest(createRequestBuilder(credentials)).map(AccessTokenRequestProvider.this::parsePayload)
                                                            .zipWith(requestTimeProvider, this::buildResponse) //
                                                            .toObservable();

        return Single.defer(() -> createHystrixCommand(requestObservable).toObservable().toSingle()) //
                     .onErrorResumeNext(error -> Single.error(unwrapHystrixException(error)));
    }

    @VisibleForTesting
    @SuppressWarnings("static-method")
    Single<Response> createRequest(final BoundRequestBuilder requestBuilder) {
        return AsyncHttpSingle.create(requestBuilder);
    }

    private <T> HystrixObservableCommand<T> createHystrixCommand(final Observable<T> requestObservable) {
        return new HystrixObservableCommand<T>(hystrixSetter) {
            @Override
            protected Observable<T> construct() {
                return requestObservable;
            }
        };
    }

    private Payload parsePayload(final Response response) {
        final int statusCode = response.getStatusCode();

        switch (statusCode) {

            case StatusCodes.OK :

                final Payload payload = parse(response.getResponseBody(), Payload.class);
                if (payload == null) {
                    throw new AccessTokenRequestException("No payload for OK response");
                }

                if (payload.accessToken == null) {
                    throw new AccessTokenRequestException("No access_token in response");
                }

                return payload;

            case StatusCodes.BAD_REQUEST :
            case StatusCodes.UNAUTHORIZED :

                BadAccessTokenException cause;
                final ErrorPayload errorPayload;
                try {
                    errorPayload = parse(response.getResponseBody(), ErrorPayload.class);
                    cause = new BadAccessTokenException(errorPayload == null ? null : errorPayload.error,
                            errorPayload == null ? null : errorPayload.errorDescription);
                } catch (final AccessTokenRequestException e) {
                    cause = new BadAccessTokenException( //
                            "HTTP status code " + statusCode, firstNonNull(e.getCause(), e));
                }

                throw new HystrixBadRequestException(cause.getMessage(), cause);
        }

        throw new AccessTokenRequestException("Unexpected status code: " + statusCode + ": "
                + response.getResponseBody());
    }

    private AccessTokenResponse buildResponse(final Payload payload, final Instant requestTime) {
        return new AccessTokenResponse(AccessToken.of(payload.accessToken),
                requestTime.plus(payload.expiresIn, ChronoUnit.SECONDS));
    }

    private List<Param> createFormParams(final UserCredentials credentials) {
        return ImmutableList.of(                                                         //
                new Param("grant_type", "password"),                                     //
                new Param("username", credentials.getApplicationUsername()),             //
                new Param("password", credentials.getApplicationPassword()),             //
                new Param("scope", Joiner.on(',').join(settings.getAccessTokenScopes())) //
            );
    }

    private static Realm createRealm(final ClientCredentials credentials) {
        return
            new Realm.Builder(credentials.getClientId(), credentials.getClientSecret()).setUsePreemptiveAuth(true) //
                                                                                       .setScheme(
                                                                                           Realm.AuthScheme.BASIC) //
                                                                                       .build();
    }

    private static Throwable unwrapHystrixException(final Throwable t) {
        if (t instanceof HystrixBadRequestException
                || (t instanceof HystrixRuntimeException
                    && ((HystrixRuntimeException) t).getFailureType() == COMMAND_EXCEPTION)) {
            return firstNonNull(t.getCause(), t);
        }

        return t;
    }
}
