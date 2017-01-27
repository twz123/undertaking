package org.zalando.undertaking.inject.guice;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.inject.HttpExchangeScopeInjectionTestBase;

import com.google.inject.Injector;
import com.google.inject.OutOfScopeException;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HttpString;

public class HttpExchangeScopeModuleTest extends HttpExchangeScopeInjectionTestBase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private Injector injector;
    private HttpExchangeScope underTest;

    @Before
    public void setUp() throws Exception {
        injector = getInjector();
        underTest = injector.getInstance(HttpExchangeScope.class);
    }

    @Test
    public void injectsRequestScopedHeaderMap() throws Exception {
        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        underTest.scoped(exchange -> {
                     TestSimpleHandler demoHandler = getNamedHandler(TestSimpleHandler.class, "testhandler", injector);

                     assertThat(demoHandler.getHeaderMap(), not(nullValue()));
                     assertThat(demoHandler.getHeaderMap().getFirst("X-Some-Header"), equalTo("blah"));
                 }).handleRequest(demoExchange);
    }

    @Test
    public void injectsRequestScopedHeaderMapOverTwoInjections() throws Exception {
        HttpServerExchange demoExchange1 = new HttpServerExchange(null);
        demoExchange1.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        underTest.scoped(exchange -> {
                     TestSimpleHandler demoHandler = getNamedHandler(TestSimpleHandler.class, "testhandler", injector);

                     assertThat(demoHandler.getHeaderMap(), is(not(nullValue())));
                     assertThat(demoHandler.getHeaderMap().getFirst("X-Some-Header"), equalTo("blah"));
                 }).handleRequest(demoExchange1);

        HttpServerExchange demoExchange2 = new HttpServerExchange(null);
        demoExchange2.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "potter");

        underTest.scoped(exchange -> {
                     TestSimpleHandler demoHandler = getNamedHandler(TestSimpleHandler.class, "testhandler", injector);

                     assertThat(demoHandler.getHeaderMap(), is(not(nullValue())));
                     assertThat(demoHandler.getHeaderMap().getFirst("X-Some-Header"), equalTo("potter"));
                 }).handleRequest(demoExchange2);
    }

    @Test
    public void injectsExchange() throws Exception {
        HttpServerExchange demoExchange = new HttpServerExchange(null);

        underTest.scoped(exchange -> {
                     TestAllInjectedHandler demoHandler = getNamedHandler(TestAllInjectedHandler.class,
                             "testAllInjectedHandler", injector);

                     assertThat(demoHandler.getExchange(), is(not(nullValue())));
                     assertThat(demoHandler.getExchange(), is(demoExchange));
                 }).handleRequest(demoExchange);
    }

    @Test
    public void injectsScopedExecutor() throws Exception {
        HttpServerExchange demoExchange = new HttpServerExchange(null);

        underTest.scoped(exchange -> {
                     TestAllInjectedHandler demoHandler = getNamedHandler(TestAllInjectedHandler.class,
                             "testAllInjectedHandler", injector);

                     assertThat(demoHandler.getExchangeScopedExecutor(), is(not(nullValue())));
                 }).handleRequest(demoExchange);
    }

    /**
     * Verifies that object explicitly bound in the `HttpExchangeScope` can not be accessed outside.
     *
     * @see  #getInjector()
     */
    @Test
    public void throwsIfHttpExchangeScopedIsUsedOutOfScope() {
        expectedException.expectCause(instanceOf(OutOfScopeException.class));
        getNamedHandler(TestSimpleHandler.class, "testhandler", injector);
    }
}
