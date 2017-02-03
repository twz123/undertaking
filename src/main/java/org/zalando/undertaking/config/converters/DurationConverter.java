package org.zalando.undertaking.config.converters;

import java.lang.reflect.Method;

import java.time.Duration;

import org.aeonbits.owner.Converter;

public class DurationConverter implements Converter<Duration> {
    @Override
    public Duration convert(final Method method, final String value) {
        if (value == null) {
            return null;
        }

        return Duration.parse(value);
    }
}
