package org.zalando.undertaking.config.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.Test;

public class PathConverterTest {
    private PathConverter underTest = new PathConverter();

    @Test
    public void convertsPath() {
        assertThat(underTest.convert(null, "/tmp")).isEqualTo(Paths.get("/tmp"));
    }

    @Test
    public void convertsNullToNull() {
        assertThat(underTest.convert(null, null)).isNull();
    }
}
