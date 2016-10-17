package org.zalando.undertaking.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;

/**
 * Deserialization strategy for those who aren't satisfied with Gson's default
 * {@code excludeFieldsWithoutExposeAnnotation()} strategy. Behaviour added by this strategy: if the {@code @Expose}
 * annotation is missing on the field it will be deserialized. To suppress field deserialization use
 * {@code @Expose(deserialize = false)}. {@code .excludeFieldsWithoutExposeAnnotation()} will take priority over this
 * class.
 */
public class ExposeDeserializationExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
        final Expose annotation = fieldAttributes.getAnnotation(Expose.class);
        return annotation != null && !annotation.deserialize();
    }

    @Override
    public boolean shouldSkipClass(final Class<?> clazz) {
        return false;
    }
}
