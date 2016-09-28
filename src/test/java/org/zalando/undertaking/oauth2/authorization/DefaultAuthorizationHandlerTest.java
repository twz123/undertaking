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

import org.mockito.runners.MockitoJUnitRunner;

import org.zalando.undertaking.inject.HttpExchangeScope;
import org.zalando.undertaking.oauth2.AuthenticationInfo;
import org.zalando.undertaking.oauth2.AuthenticationInfoSettings;
import org.zalando.undertaking.oauth2.authorization.DefaultAuthorizationHandler.Settings;
import org.zalando.undertaking.problem.ProblemHandlerBuilder;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import rx.Single;

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

    @Mock
    private AuthenticationInfoSettings authInfoSettings;

    private DefaultAuthorizationHandler underTest;

    @Mock
    private AuthenticationInfo authInfo;

    @Mock
    private Predicate<AuthenticationInfo> authPredicate;

    @Mock
    private HttpHandler next;

    @Before
    public void initializeTest() {
        when(settings.getTimeout()).thenReturn(Duration.ofMinutes(1));
        when(scope.scoped(any())).then(invocation -> invocation.getArgument(0));
        when(authInfoProvider.get()).thenReturn(Single.just(authInfo));

        underTest = spy(new DefaultAuthorizationHandler(settings, scope, authInfoProvider, problemBuilder,
                    authInfoSettings));
    }

    @Test
    public void simpleAuthSuccess() throws Exception {
        when(authPredicate.test(authInfo)).thenReturn(true);

        final HttpServerExchange exchange = new HttpServerExchange(null);
        doAnswer(invocation -> exchange.dispatch()).when(next).handleRequest(exchange);

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate, next);
        inOrder.verify(authPredicate).test(authInfo);
        inOrder.verify(next).handleRequest(exchange);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void authFailure() throws Exception {
        when(authPredicate.test(authInfo)).thenReturn(false);

        final HttpServerExchange exchange = new HttpServerExchange(null);

        final HttpHandler forbidden = mock(HttpHandler.class);
        doAnswer(invocation -> exchange.dispatch()).when(forbidden).handleRequest(exchange);
        doAnswer(invocation -> forbidden).when(underTest).forbidden(same(authInfo), any());

        underTest.require(authPredicate, next).handleRequest(exchange);

        final InOrder inOrder = inOrder(authPredicate, forbidden, next);
        inOrder.verify(authPredicate).test(authInfo);
        inOrder.verify(forbidden).handleRequest(exchange);
        inOrder.verifyNoMoreInteractions();
    }
}