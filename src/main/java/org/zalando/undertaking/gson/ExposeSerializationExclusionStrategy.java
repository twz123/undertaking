package org.zalando.undertaking.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;

/**
 * Serialization strategy for those who don't satisfied with default {@code excludeFieldsWithoutExposeAnnotation()}
 * strategy. Behaviour added by this strategy: the {@code @Expose} annotation is missing on the field it will be
 * serialized. To suppress field serialization use {@code @Expose(serialize = false)}
 * {@code .excludeFieldsWithoutExposeAnnotation()} will take priority over this class.
 */
public class ExposeSerializationExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
        Expose annotation = fieldAttributes.getAnnotation(Expose.class);
        return annotation != null && !annotation.serialize();
    }

    @Override
    public boolean shouldSkipClass(final Class<?> clazz) {
        return false;
    }
}
