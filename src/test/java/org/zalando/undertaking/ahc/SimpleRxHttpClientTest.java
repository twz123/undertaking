package org.zalando.undertaking.ahc;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import rx.Single;

@RunWith(MockitoJUnitRunner.class)
public class SimpleRxHttpClientTest {

    @Mock
    private AsyncHttpClient ahc;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    @Captor
    private ArgumentCaptor<? extends AsyncHandler<?>> handlerCaptor;

    @InjectMocks
    private SimpleRxHttpClient underTest;

    @Before
    public void initializeTest() {
        when(ahc.executeRequest(requestCaptor.capture(), handlerCaptor.capture())).then(invocation -> {
            final AsyncHandler<Object> argument = invocation.getArgument(1);
            return CompletableFuture.completedFuture(argument.onCompleted());
        });
    }

    @Test
    public void toStringContainsUsefulInfo() {
        assertThat(underTest.toString(), stringContainsInOrder(Arrays.asList("SimpleRxHttpClient", "ahc")));
    }

    @Test
    public void executesRequest() {
        final Request request = Dsl.get("http://example.com").build();

        final Single<Response> requestSingle = underTest.prepareRequest(request);
        verifyZeroInteractions(ahc);

        requestSingle.toBlocking().value();

        assertThat(requestCaptor.getAllValues(), contains(request));
        assertThat(handlerCaptor.getAllValues(), hasSize(1));

        final AsyncHandler<?> lastHandler = handlerCaptor.getValue();

        requestSingle.toBlocking().value();

        assertThat(requestCaptor.getAllValues(), contains(request, request));
        assertThat(handlerCaptor.getAllValues(), contains(is(lastHandler), not(lastHandler)));
    }
}
