package org.zalando.undertaking.oauth2.credentials;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import java.nio.file.Files;

import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.MoreObjects;

import io.reactivex.Single;

@RunWith(MockitoJUnitRunner.class)
public class CredentialsProviderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private CredentialsSettings config;

    private Provider<Single<TestCredentials>> underTest;

    private File credentialsFile;

    private static void writeToCredentialsFile(final File credentialsFile, final String foo, final String bar)
        throws IOException {
        try(final Writer writer = Files.newBufferedWriter(credentialsFile.toPath())) {
            writer.write(String.format("{\"foo\": \"%s\", \"bar\": \"%s\"}", foo, bar));
        }
    }

    @Before
    public void setUp() throws IOException {
        when(config.getCredentialsDirectory()).thenReturn(temporaryFolder.getRoot().toPath());

        credentialsFile = createTestCredentialsFile();
        underTest = new TestCredentialsProvider(credentialsFile.getName(), config);
    }

    @Test
    public void readsCredentialsFromFile() {
        underTest.get().test().awaitDone(10, TimeUnit.SECONDS).assertValue(createCredentials("bar", "foo"));
    }

    private File createTestCredentialsFile() throws IOException {
        final File credentialsFile = temporaryFolder.newFile();

        writeToCredentialsFile(credentialsFile, "bar", "foo");

        return credentialsFile;
    }

    private TestCredentials createCredentials(final String foo, final String bar) {
        final TestCredentials credentials = new TestCredentials();
        credentials.setFoo(foo);
        credentials.setBar(bar);

        return credentials;
    }

    @SuppressWarnings("unused")
    static class TestCredentials {
        private String foo;

        private String bar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(final String bar) {
            this.bar = bar;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestCredentials that = (TestCredentials) o;

            return foo.equals(that.foo) && bar.equals(that.bar);
        }

        @Override
        public int hashCode() {
            int result = foo.hashCode();
            result = 31 * result + bar.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this) //
                              .add("foo", foo)      //
                              .add("bar", bar)      //
                              .toString();
        }
    }

    static class TestCredentialsProvider extends CredentialsProvider<TestCredentials> {
        public TestCredentialsProvider(final String filename, final CredentialsSettings config) {
            super(filename, config);
        }
    }
}
