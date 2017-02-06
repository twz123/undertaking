package org.zalando.undertaking.config.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

public class FromStringConverterTest {
    private FromStringConverter fromStringConverter;

    @Before
    public void setUp() throws Exception {
        fromStringConverter = new FromStringConverter();
    }

    @Test
    public void throwsOnInvalidClass() throws NoSuchMethodException {
        Method method = getTestInterfaceMethod("withoutFromString");

        //J-
        assertThatThrownBy(() -> fromStringConverter.convert(method, "irrelevant"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("to have a single-argument method named 'fromString'");
        //J+
    }

    @Test
    public void throwsOnNonStaticFromString() throws NoSuchMethodException {
        Method method = getTestInterfaceMethod("withoutStaticFromString");

        //J-
        assertThatThrownBy(() -> fromStringConverter.convert(method, "irrelevant"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not static");
        //J+
    }

    @Test
    public void throwsOnInvalidReturnType() throws NoSuchMethodException {
        Method method = getTestInterfaceMethod("invalidReturnType");

        //J-
        assertThatThrownBy(() -> fromStringConverter.convert(method, "irrelevant"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not return an instance of org.zalando.undertaking.config.converters.FromStringConverterTest$ClassWithInvalidReturnType");
        //J+
    }

    @Test
    public void throwsOnInvalidParameterCount() throws NoSuchMethodException {
        Method method = getTestInterfaceMethod("invalidParameterCount");

        //J-
        assertThatThrownBy(() -> fromStringConverter.convert(method, "irrelevant"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("to have a single-argument method named 'fromString'");
        //J+
    }

    @Test
    public void throwsOnInvalidParameterType() throws NoSuchMethodException {
        Method method = getTestInterfaceMethod("invalidParameterType");

        //J-
        assertThatThrownBy(() -> fromStringConverter.convert(method, "irrelevant"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("to have a single-argument method named 'fromString'");
        //J+
    }

    @Test
    public void throwsOnThrowingFromString() throws NoSuchMethodException {
        Method method = getTestInterfaceMethod("throwingFromString");

        //J-
        assertThatThrownBy(() -> fromStringConverter.convert(method, "irrelevant"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has thrown an exception")
            .hasCauseInstanceOf(NullPointerException.class);
        //J+
    }

    @Test
    public void successfullyCallsFromString() throws NoSuchMethodException {
        Object result = fromStringConverter.convert(getTestInterfaceMethod("valid"), "aStringValue");

        assertThat(result).isInstanceOf(ClassWithFromString.class);
        assertThat(((ClassWithFromString) result).getValue()).isEqualTo("aStringValue");
    }

    private Method getTestInterfaceMethod(final String name) throws NoSuchMethodException {
        return TestConfig.class.getDeclaredMethod(name);
    }

    public interface TestConfig {
        ClassWithoutFromString withoutFromString();

        ClassWithNonStaticFromString withoutStaticFromString();

        ClassWithInvalidReturnType invalidReturnType();

        ClassWithInvalidParameterType invalidParameterType();

        ClassWithInvalidParameterCount invalidParameterCount();

        ClassWithThrowingFromString throwingFromString();

        ClassWithFromString valid();
    }

    static class ClassWithoutFromString {
        // Has no from string method.
    }

    static class ClassWithNonStaticFromString {
        public ClassWithNonStaticFromString fromString(final String s) {
            return new ClassWithNonStaticFromString();
        }
    }

    static class ClassWithInvalidReturnType {
        public static String fromString(final String s) {
            return "from string";
        }
    }

    static class ClassWithInvalidParameterType {
        public static ClassWithInvalidParameterType fromString(final int s) {
            return new ClassWithInvalidParameterType();
        }
    }

    static class ClassWithInvalidParameterCount {
        public static ClassWithInvalidParameterType fromString(final String s1, final String s2) {
            return new ClassWithInvalidParameterType();
        }
    }

    static class ClassWithFromString {
        private String value;

        public ClassWithFromString(final String value) {
            this.value = value;
        }

        public static ClassWithFromString fromString(final String s) {
            return new ClassWithFromString(s);
        }

        public String getValue() {
            return value;
        }
    }

    static class ClassWithThrowingFromString {
        public static ClassWithThrowingFromString fromString(final String s) {
            throw new NullPointerException("bad");
        }
    }
}
