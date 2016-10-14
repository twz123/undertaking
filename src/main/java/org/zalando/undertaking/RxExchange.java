package org.zalando.undertaking;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.io.Receiver.ErrorCallback;
import io.undertow.io.Receiver.FullStringCallback;

import io.undertow.server.Connectors;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.util.SameThreadExecutor;
import io.undertow.util.StatusCodes;

import rx.Single;
import rx.Subscriber;

import rx.subjects.AsyncSubject;

/**
 * RxJava style interactions with Undertow's {@code HttpServerExchange} class.
 */
public final class RxExchange {

    private static final Logger LOG = LoggerFactory.getLogger(RxExchange.class);

    /**
     * Dispatches the given {@code exchange} to the {@code HttpHandler} emitted by the given {@code handlerSingle}.
     *
     * <p>That {@code Single} should never emit an error if proper error handling is desired. If an error is emitted,
     * the HTTP status set to {@code 500 Internal Server Error} and the exchange is
     * {@linkplain HttpServerExchange#endExchange() ended}. To add proper error handling, add some error handlers to the
     * {@code Single} itself to return {@code HttpHandlers} that send correct error responses:
     *
     * <ul>
     *   <li>{@link Single#onErrorReturn}</li>
     *   <li>{@link Single#onErrorResumeNext}</li>
     * </ul>
     * </p>
     *
     * @param   handlerSingle  emits the handler that will be used for dispatching
     * @param   exchange       the exchange that is being dispatched
     *
     * @throws  NullPointerException  if at least one of the arguments is {@code null}
     */
    public static void dispatch(final Single<HttpHandler> handlerSingle, final HttpServerExchange exchange) {
        exchange.dispatch(SameThreadExecutor.INSTANCE, new SingleDispatch(handlerSingle, exchange));
    }

    /**
     * Creates a {@code HttpHandler} that dispatches {@code HttpServerExchanges} to the {@code HttpHandler} emitted by
     * {@code handlerSingle}.
     *
     * @param   handlerSingle  the {@code Single} whose emitted {@code HttpHandler} will be used to handle requests
     *
     * @return  a {@code HttpHandler} that delegates request processing to another {@code HttpHandler} that is provided
     *          in a reactive way
     *
     * @throws  NullPointerException  if {@code handlerSingle} is {@code null}
     */
    public static HttpHandler dispatchTo(final Single<HttpHandler> handlerSingle) {
        return new RxDispatchingHttpHandler(handlerSingle);
    }

    /**
     * Reads the request body of {@code exchange} and emits it as a {@code String} when it has been fully read. The
     * string will be decoded according to the charset specified by the {@code Content-Type} HTTP request header. If
     * that header is absent or doesn't specify any charset,
     * {@link java.nio.charset.StandardCharsets#ISO_8859_1 ISO-8859-1} will be used. Emits an error if request reading
     * failed.
     *
     * <p>Usage example:
     *
     * <pre>
     * {@code dispatch(receiveFullString(exchange).map(payload -> {
     *      return xc -> {
     *          xc.setStatusCode(StatusCodes.OK);
     *          xc.getResponseSender().send(payload);
     *      };
     *  }), exchange);}
     * </pre>
     * </p>
     *
     * @param   exchange  exchange whose request body shall be emitted
     *
     * @return  a <em>hot</em> {@code Single} that emits the HTTP request body of the given {@code exchange} as a
     *          {@code String}, or an error if request reading fails
     */
    public static Single<String> receiveFullString(final HttpServerExchange exchange) {
        final Charset requestCharset;
        try {
            requestCharset = getRequestCharset(exchange);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
            return Single.error(e);
        }

        final FullStringConsumer consumer = new FullStringConsumer();
        exchange.getRequestReceiver().receiveFullString(consumer, consumer, requestCharset);
        return consumer.toSingle();
    }

    /**
     * @throws  IllegalCharsetNameException  if the charset name specified by the request is illegal
     * @throws  UnsupportedCharsetException  if no support for the charset specified by the request is available in this
     *                                       instance of the Java virtual machine
     */
    private static Charset getRequestCharset(final HttpServerExchange exchange) {
        final String requestCharset = exchange.getRequestCharset();
        return requestCharset == null ? ISO_8859_1 : Charset.forName(requestCharset);
    }

    private RxExchange() {
        throw new AssertionError("No instances for you!");
    }

    private static final class SingleDispatch extends Subscriber<HttpHandler> implements Runnable,
        ExchangeCompletionListener {

        private final Single<HttpHandler> handlerSingle;
        private final HttpServerExchange exchange;

        SingleDispatch(final Single<HttpHandler> handlerSingle, final HttpServerExchange exchange) {
            this.handlerSingle = requireNonNull(handlerSingle);
            this.exchange = requireNonNull(exchange);
        }

        // Initial dispatch action, we may safely assume that this gets only called once.
        @Override
        public void run() {
            exchange.addExchangeCompleteListener(this);
            handlerSingle.subscribe(this);
        }

        // The Single emitted the handler to continue request processing.
        @Override
        public void onNext(final HttpHandler handler) {
            unsubscribe();
            Connectors.executeRootHandler(handler, exchange);
        }

        // The Single errored out, not much that we can do about that.
        @Override
        public void onError(final Throwable error) {
            LOG.error("Error occurred while dispatching request: [{}]", error.getMessage(), error);
            if (!exchange.isResponseStarted()) {
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            }

            unsubscribe();
            exchange.endExchange();
        }

        @Override
        public void onCompleted() {
            // deliberately ignored
        }

        // Called when the HTTP exchange is completed. Used to unsubscribe from handlerSingle if the exchange
        // completed before a handler has been emitted.
        @Override
        public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
            try {
                unsubscribe();
            } finally {
                nextListener.proceed();
            }
        }
    }

    private static final class RxDispatchingHttpHandler implements HttpHandler {
        private final Single<HttpHandler> handlerSingle;

        RxDispatchingHttpHandler(final Single<HttpHandler> handlerSingle) {
            this.handlerSingle = requireNonNull(handlerSingle);
        }

        @Override
        public String toString() {
            return "RxExchange.dispatchTo(" + handlerSingle + ')';
        }

        @Override
        public void handleRequest(final HttpServerExchange exchange) {
            dispatch(handlerSingle, exchange);
        }
    }

    private static final class FullStringConsumer implements FullStringCallback, ErrorCallback {
        private final AsyncSubject<String> payloadSubject;

        FullStringConsumer() {
            payloadSubject = AsyncSubject.create();
        }

        @Override
        public void handle(final HttpServerExchange exchange, final String payload) {
            exchange.dispatch(SameThreadExecutor.INSTANCE,
                () -> {
                    payloadSubject.onNext(payload);
                    payloadSubject.onCompleted();
                });
        }

        @Override
        public void error(final HttpServerExchange exchange, final IOException e) {
            payloadSubject.onError(e);
        }

        Single<String> toSingle() {
            return payloadSubject.toSingle();
        }
    }
}
