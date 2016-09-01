package org.zalando.undertaking.oauth2.credentials;

import java.io.IOException;
import java.io.Reader;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rx.Single;

import rx.schedulers.Schedulers;

/**
 * Provides credential instances of a particular type.
 *
 * @param  <T>  type of credentials this provider manages
 */
public abstract class CredentialsProvider<T> implements Provider<Single<T>> {

    /**
     * Logging instance of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CredentialsProvider.class);

    /**
     * Path to the file containing the credentials.
     */
    private final Path credentialsPath;

    /**
     * Used to deserialize the credentials file into its object representation.
     */
    private final Gson gson;

    private final TypeToken<T> typeToken;

    /**
     * Creates a new provider to load credentials of type {@code T} from a specific file.
     */
    public CredentialsProvider(final String filename, final CredentialsSettings settings) {
        typeToken = new TypeToken<T>(getClass()) { };
        gson = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        credentialsPath = settings.getCredentialsDirectory().resolve(filename).normalize().toAbsolutePath();
    }

    @Override
    public Single<T> get() {
        return Single.fromCallable(this::loadCredentials).subscribeOn(Schedulers.io());
    }

    /**
     * Reads and deserializes the content read from {@link #credentialsPath}.
     *
     * @throws  IOException  if the file cannot be opened or read for whatever reason
     */
    private T loadCredentials() throws IOException {
        try(final Reader reader = Files.newBufferedReader(credentialsPath)) {
            LOG.debug("Loading credentials from [{}]", credentialsPath);
            return gson.fromJson(reader, typeToken.getType());
        }
    }
}
