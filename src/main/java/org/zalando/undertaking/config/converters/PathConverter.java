package org.zalando.undertaking.config.converters;

import java.lang.reflect.Method;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.aeonbits.owner.Converter;

public class PathConverter implements Converter<Path> {

    @Override
    public Path convert(final Method method, final String value) {
        if (value == null) {
            return null;
        }

        return Paths.get(value);
    }
}
