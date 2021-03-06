package org.zalando.undertaking.oauth2;

import org.asynchttpclient.AsyncHttpClient;

import org.zalando.undertaking.request.RequestProvider;

import com.google.gson.JsonSyntaxException;

class OAuth2RequestProvider extends RequestProvider {

    /**
     * Payload, which is received from OAuth2 services in case of an error.
     */
    static final class ErrorPayload {
        protected String error;
        protected String errorDescription;
    }

    public OAuth2RequestProvider(final AsyncHttpClient client) {
        super(client);
    }

    protected <T> T parse(final String payload, final Class<T> clazz) {
        try {
            return gson.fromJson(payload, clazz);
        } catch (final JsonSyntaxException e) {
            throw new AccessTokenRequestException("Failed to parse JSON payload", e);
        }
    }
}
