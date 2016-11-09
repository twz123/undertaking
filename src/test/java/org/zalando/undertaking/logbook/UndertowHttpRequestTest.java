package org.zalando.undertaking.logbook;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.undertow.server.HttpServerExchange;

public class UndertowHttpRequestTest {

    private final Function<String, String> obfuscator = path -> {
        final Matcher matcher = Pattern.compile("(/secret-codes/)([^/]+)").matcher(path);
        if (matcher.find()) {
            final String code = matcher.group(2);
            final HashCode hashedCode = Hashing.sha1().hashString(code, StandardCharsets.UTF_8);
            final StringBuffer buf = new StringBuffer();
            matcher.appendReplacement(buf, String.format("%s{sha1:%s}", matcher.group(1), hashedCode));
            matcher.appendTail(buf);
            return buf.toString();
        }

        return path;
    };

    private final HttpServerExchange httpServerExchange = new HttpServerExchange(null);
    private final UndertowHttpRequest underTest = new UndertowHttpRequest(httpServerExchange, obfuscator);

    @Test
    public void getRequestUri() {
        final String code = "confidential";
        httpServerExchange.setRequestURI(String.format("https://secret-codes/%s/redemptions", code));
        assertThat(underTest.getRequestUri(),
            equalTo(
                String.format("https://secret-codes/{sha1:%s}/redemptions",
                    Hashing.sha1().hashString(code, StandardCharsets.UTF_8))));

    }
}
