package org.zalando.undertaking.request;

import static java.util.Objects.requireNonNull;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

import org.asynchttpclient.AsyncHttpClient;

import org.zalando.undertaking.oauth2.AccessTokenRequestException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Allows child classes to create requests using {@code AsyncHttpClient} and JSON parsing via {@code Gson}.
 */
public abstract class RequestProvider {

    protected final AsyncHttpClient httpClient;
    protected final Gson gson;

    public RequestProvider(final AsyncHttpClient httpClient) {
        this(httpClient, new GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES).create());
    }

    public RequestProvider(final AsyncHttpClient httpClient, final Gson gson) {
        this.httpClient = requireNonNull(httpClient);
        this.gson = requireNonNull(gson);
    }

    protected <T> T parse(final String payload, final Class<T> clazz) {
        try {
            return gson.fromJson(payload, clazz);
        } catch (final JsonSyntaxException e) {
            throw new AccessTokenRequestException("Failed to parse JSON payload", e);
        }
    }
}
