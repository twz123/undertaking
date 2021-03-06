package org.zalando.undertaking.oauth2.authorization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Duration;

import java.util.function.Predicate;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.mockito.junit.MockitoJUnitRunner;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.oauth2.AuthenticationInfo;
import org.zalando.undertaking.oauth2.authorization.DefaultAuthorizationHandler.Settings;
import org.zalando.undertaking.problem.ProblemHandlerBuilder;

import io.reactivex.Single;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;

import io.undertow.util.HttpString;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthorizationHandlerTest {

    @Mock
    private Settings settings;

    @Mock
    private HttpExchangeScope scope;

    @Mock
    private Provider<Single<AuthenticationInfo>> authInfoProvider;

    @Mock
    private Provider<ProblemHandlerBuilder> problemBuilder;

    private DefaultAuthorizationHandler underTest;

    @Mock
    private Predicate<AuthenticationInfo> authPredicate;

    @Mock
    private HttpHandler next;

    private AuthenticationInfo authInfo = AuthenticationInfo.builder().build();

    @Before
    public void initializeTest() {
        when(settings.getTimeout()).thenReturn(Duration.ofMinutes(1));
        when(scope.scoped(any())).then(invocation -> invocation.getArgument(0));
        when(authInfoProvider.get()).thenReturn(Single.defer(() -> Single.just(authInfo)));

        underTest = spy(new DefaultAuthorizationHandler(settings, scope, authInfoProvider, problemBuilder));
    }

    @Test
    public void simpleAuthSuccess() throws Exception {
        when(authPredicate.test(authInfo)).thenReturn(true);

        final HttpServerExchange exchange = getExchange();

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate, exchange);
        inOrder.verify(authPredicate).test(authInfo);
        inOrder.verify(exchange).dispatch(next);
    }

    @Test
    public void authFailure() throws Exception {
        when(authPredicate.test(authInfo)).thenReturn(false);

        final HttpServerExchange exchange = getExchange();

        final HttpHandler forbidden = mock(HttpHandler.class);
        doAnswer(invocation -> forbidden).when(underTest).forbidden(same(authInfo), any());

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate);
        inOrder.verify(authPredicate).test(authInfo);
    }

    @Test
    public void authorizesBusinessPartnerOverrides() throws Exception {
        when(settings.getBusinessPartnerIdOverrideHeader()).thenReturn("BP-Override");
        when(settings.getBusinessPartnerIdOverrideScope()).thenReturn("bp_override");

        authInfo = authInfo.with().scopes("bp_override").build();
        when(authPredicate.test(authInfo)).thenReturn(true);

        final HttpServerExchange exchange = getExchange();
        exchange.getRequestHeaders().put(new HttpString("BP-Override"), "Someone Else");

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate, exchange);
        inOrder.verify(authPredicate).test(authInfo);
        inOrder.verify(exchange).dispatch(next);
    }

    @Test
    public void authorizesNonBusinessPartnerOverrides() throws Exception {
        when(settings.getBusinessPartnerIdOverrideHeader()).thenReturn("BP-Override");
        when(authPredicate.test(authInfo)).thenReturn(true);

        final HttpServerExchange exchange = getExchange();

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate, next, exchange);
        inOrder.verify(authPredicate).test(authInfo);
        inOrder.verify(exchange).dispatch(next);
    }

    @Test
    public void forbidsBusinessPartnerOverrideOnMissingScope() throws Exception {
        when(settings.getBusinessPartnerIdOverrideHeader()).thenReturn("BP-Override");
        when(settings.getBusinessPartnerIdOverrideScope()).thenReturn("bp_override");

        final HttpServerExchange exchange = getExchange();
        exchange.getRequestHeaders().put(new HttpString("BP-Override"), "Someone Else");

        final HttpHandler forbidden = mock(HttpHandler.class);
        doAnswer(invocation -> forbidden).when(underTest).forbidden(same(authInfo), any());

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate, forbidden, next, exchange);
        inOrder.verify(exchange).dispatch(forbidden);
    }

    @Test
    public void forbidsBusinessPartnerOverrideIfConfiguredScopeIsNull() throws Exception {
        when(settings.getBusinessPartnerIdOverrideHeader()).thenReturn("BP-Override");

        final HttpServerExchange exchange = getExchange();

        exchange.getRequestHeaders().put(new HttpString("BP-Override"), "Someone Else");

        final HttpHandler forbidden = mock(HttpHandler.class);
        doAnswer(invocation -> forbidden).when(underTest).forbidden(same(authInfo), any());

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(exchange);
        inOrder.verify(exchange).dispatch(forbidden);
    }

    private HttpServerExchange getExchange() {
        return spy(new HttpServerExchange(mock(ServerConnection.class, Mockito.RETURNS_DEEP_STUBS)));
    }
}
