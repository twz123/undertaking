package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import org.asynchttpclient.extras.rxjava.single.AsyncHttpSingle;

import org.zalando.undertaking.inject.Request;

import com.google.common.net.HttpHeaders;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

import rx.Observable;
import rx.Single;

class TokenInfoRequestProvider extends OAuth2RequestProvider {

    private static final class Payload {
        Set<String> scope;
        String uid;
    }

    private static final HystrixObservableCommand.Setter SETTER =
        HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("auth")) //
                                       .andCommandKey(HystrixCommandKey.Factory.asKey("tokenInfo"));

    private final HeaderMap requestHeaders;

    private final AuthenticationInfoSettings settings;

    @Inject
    public TokenInfoRequestProvider(final AuthenticationInfoSettings settings, final AsyncHttpClient client,
            @Request final HeaderMap requestHeaders) {
        super(client);
        this.settings = requireNonNull(settings);
        this.requestHeaders = requireNonNull(requestHeaders);
    }

    public HystrixObservableCommand<AuthenticationInfo> createCommand(final AccessToken accessToken) {
        final BoundRequestBuilder requestBuilder = buildRequest(accessToken);

        return new HystrixObservableCommand<AuthenticationInfo>(SETTER) {
            @Override
            protected Observable<AuthenticationInfo> construct() {
                return TokenInfoRequestProvider.this.construct(requestBuilder);
            }
        };
    }

    private BoundRequestBuilder buildRequest(final AccessToken accessToken) {
        return
            httpClient.prepareGet(settings.getTokenInfoEndpoint().toString()) //
                      .setHeader(HttpHeaders.ACCEPT, "application/json")      //
                      .addQueryParam("access_token", accessToken.getValue());
    }

    Observable<AuthenticationInfo> construct(final BoundRequestBuilder requestBuilder) {
        return
            AsyncHttpSingle.create(requestBuilder)                                //
                           .onErrorResumeNext(TokenInfoRequestProvider::mapError) //
                           .map(this::parseResponse)                              //
                           .toObservable();
    }

    private static <T> Single<T> mapError(final Throwable error) {
        return Single.error(new TokenInfoRequestException(error.getMessage(), error));
    }

    private AuthenticationInfo parseResponse(final Response response) {
        final int statusCode = response.getStatusCode();
        switch (statusCode) {

            case StatusCodes.OK :

                final Payload payload = parse(response.getResponseBody(), Payload.class);
                return new AuthenticationInfo(Optional.ofNullable(payload.uid), payload.scope,
                        Optional.ofNullable(requestHeaders.getFirst(settings.getBusinessPartnerIdOverrideHeader())));

            case StatusCodes.BAD_REQUEST :
            case StatusCodes.UNAUTHORIZED :

                final ErrorPayload errorPayload = parse(response.getResponseBody(), ErrorPayload.class);

                // Fix wrong error from Zalando endpoint
                if ("invalid_request".equals(errorPayload.error)
                        && "Access Token not valid".equals(errorPayload.errorDescription)) {
                    final BadTokenInfoException badTokenInfoException = //
                        new BadTokenInfoException("invalid_token", "Access Token not valid.");
                    throw new HystrixBadRequestException(badTokenInfoException.getMessage(), badTokenInfoException);
                }

                throw new HystrixBadRequestException(errorPayload.errorDescription,
                    new BadTokenInfoException(errorPayload.error, errorPayload.errorDescription));
        }

        throw new TokenInfoRequestException("Unsupported status code: " + statusCode + ": "
                + response.getResponseBody());
    }
}
