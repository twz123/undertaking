package org.zalando.undertaking.config.converters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.aeonbits.owner.Converter;

public class FromStringConverter<T> implements Converter<T> {
    @Override
    @SuppressWarnings("unchecked")
    public T convert(final Method method, final String s) {
        Class<?> returnType = method.getReturnType();

        try {
            Method fromString = returnType.getDeclaredMethod("fromString", String.class);
            if (!Modifier.isStatic(fromString.getModifiers())) {
                throw new IllegalArgumentException("fromString method of class " + returnType.getName()
                        + " is not static");
            }

            if (!fromString.getReturnType().isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException("fromString method of class " + returnType.getName()
                        + " does not return an instance of " + returnType.getName());
            }

            try {
                return (T) fromString.invoke(null, s);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("fromString method has thrown an exception", e.getCause());
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Expecting class of type " + returnType.getName()
                    + " to have a single-argument method named 'fromString'");
        }
    }
}
