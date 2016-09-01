package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

import org.asynchttpclient.AsyncHttpClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Allows child classes to create requests using {@code AsyncHttpClient} and JSON parsing via {@code Gson}.
 */
abstract class RequestProvider {

    /**
     * Payload, which is received from OAuth2 services in case of an error.
     */
    static final class ErrorPayload {
        String error;
        String errorDescription;
    }

    protected final AsyncHttpClient client;
    protected final OAuth2Settings settings;
    protected final Gson gson;

    public RequestProvider(final AsyncHttpClient client, final OAuth2Settings settings) {
        this.settings = requireNonNull(settings);
        this.client = requireNonNull(client);
        this.gson = new GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES).create();
    }

    protected <T> T parse(final String payload, final Class<T> clazz) {
        try {
            return gson.fromJson(payload, clazz);
        } catch (final JsonSyntaxException e) {
            throw new AccessTokenRequestException("Failed to parse JSON payload", e);
        }
    }
}
