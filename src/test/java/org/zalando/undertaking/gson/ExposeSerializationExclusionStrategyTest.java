package org.zalando.undertaking.gson;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class ExposeSerializationExclusionStrategyTest {

    private final Model model = new Model();

    private static Gson gson;

    @BeforeClass
    public static void configureWithSerializationExclusionStrategy() {
        gson = new GsonBuilder().addSerializationExclusionStrategy(new ExposeSerializationExclusionStrategy()).create();
    }

    @Test
    public void shouldSkipNonExposedField() throws Exception {
        final String serialized = gson.toJson(model);
        assertThat("Non serializable field serialized", serialized.contains("nonExposed"), is(false));
        assertThat("Non serializable field content serialized", serialized.contains(model.nonExposed), is(false));
    }

    @Test
    public void shouldSerializeExposedField() throws Exception {
        final String serialized = gson.toJson(model);
        assertThat("Explicit exposed field missing after serialization", serialized.contains("explicitExposure"),
            is(true));
        assertThat("Explicit exposed field content missing after serialization",
            serialized.contains(model.implicitExposure), is(true));
    }

    @Test
    public void shouldSerializeNonAnnotatedField() throws Exception {
        final String serialized = gson.toJson(model);
        assertThat("Implicit exposed field missing after serialization", serialized.contains("implicitExposure"),
            is(true));

        assertThat("Implicit exposed field content missing after serialization",
            serialized.contains(model.explicitExposure), equalTo(true));
    }

    private class Model {
        private String implicitExposure = "Exposed implicitly";

        @Expose(serialize = true)
        private String explicitExposure = "Exposed explicitly";

        @Expose(serialize = false)
        private String nonExposed = "Non exposed";
    }
}
