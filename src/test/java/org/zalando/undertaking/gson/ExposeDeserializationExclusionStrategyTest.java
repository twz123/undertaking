package org.zalando.undertaking.gson;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class ExposeDeserializationExclusionStrategyTest {
    private final String SOURCE = "{"           //
            + "\"implicitExposure\": \"here\"," //
            + "\"explicitExposure\": \"here\"," //
            + "\"noExposure\": \"here\""        //
            + "}";

    private static Gson gson;

    @BeforeClass
    public static void configureWithSerializationExclusionStrategy() {
        gson = new GsonBuilder().addDeserializationExclusionStrategy(new ExposeDeserializationExclusionStrategy())
                                .create();
    }

    @Test
    public void shouldDeserializeFieldWithImplicitExposure() throws Exception {
        final Model model = gson.fromJson(SOURCE, Model.class);
        assertThat(model, is(notNullValue()));
        assertThat(model.implicitExposure, is("here"));
    }

    @Test
    public void shouldDeserializeFieldWithExplicitExposure() throws Exception {
        final Model model = gson.fromJson(SOURCE, Model.class);
        assertThat(model, is(notNullValue()));
        assertThat(model.explicitExposure, is("here"));
    }

    @Test
    public void shouldNotDeserializeFieldWithoutExposure() throws Exception {
        final Model model = gson.fromJson(SOURCE, Model.class);
        assertThat(model, is(notNullValue()));
        assertThat(model.noExposure, is(nullValue()));
    }

    private class Model {
        private String implicitExposure;

        @Expose(deserialize = true)
        private String explicitExposure;

        @Expose(deserialize = false)
        private String noExposure;
    }

}
