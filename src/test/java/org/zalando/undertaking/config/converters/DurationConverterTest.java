package org.zalando.undertaking.config.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

public class DurationConverterTest {
    private DurationConverter underTest = new DurationConverter();

    @Test
    public void convertsSeconds() {
        assertThat(underTest.convert(null, "PT1S")).isEqualByComparingTo(Duration.of(1, ChronoUnit.SECONDS));
    }

    @Test
    public void convertsNullToNull() {
        assertThat(underTest.convert(null, null)).isNull();
    }
}
