package org.zalando.undertaking.metrics;

import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;

public final class MetricNameNormalizer {
    private static final CharMatcher CURLY_BRACES = CharMatcher.anyOf("{}");
    private static final Pattern SLASH_DASH_OR_DASH_SLASH = Pattern.compile("/-|-/");
    private static final CharMatcher SLASH = CharMatcher.is('/');
    private static final CharMatcher DOT = CharMatcher.is('.');
    private static final CharMatcher DASHES = CharMatcher.anyOf("-_");

    public static String normalize(String value) {
        value = CURLY_BRACES.replaceFrom(value, '-');
        value = SLASH_DASH_OR_DASH_SLASH.matcher(value).replaceAll("/");
        value = SLASH.replaceFrom(value, '.');
        value = DOT.collapseFrom(value, '.');
        value = DOT.trimFrom(value);
        return DASHES.trimFrom(value);
    }
}
