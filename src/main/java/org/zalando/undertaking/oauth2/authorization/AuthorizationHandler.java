package org.zalando.undertaking.oauth2.authorization;

import java.util.function.Function;
import java.util.function.Predicate;

import org.zalando.undertaking.oauth2.AuthenticationInfo;

import io.reactivex.Single;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Handles authorization by evaluating predicates against the {@code AuthenticationInfo} of a HTTP request. If a request
 * is authorized, request processing is passed to a {@literal "next"} handler. If a request is unauthorized, sends an
 * appropriate error response to clients.
 */
public interface AuthorizationHandler {

    /**
     * Passes request processing to {@code next} if a HTTP request meets the requirements imposed by {@code predicate}.
     *
     * @param   predicate  requirements that the HTTP request must fulfill in order to be authorized
     * @param   next {@code HttpHandler} to which request processing will be passed on if the HTTP request is authorized
     *
     * @return  a {@code HttpHandler} that, if authorized, delegates request processing to {@code next}, or, if
     *          unauthorized, to a {@code HttpHandler} that sends an appropriate error response to clients.
     *
     * @throws  NullPointerException  if at least one of the parameters is {@code null}
     */
    HttpHandler require(Predicate<? super AuthenticationInfo> predicate, HttpHandler next);

    /**
     * Passes request processing to the handler emitted by the {@code Single} returned by {@code nextProvider} if a HTTP
     * request meets the requirements imposed by {@code predicate}.
     *
     * <p>This method may be used in order to start processing requests in parallel to the authorization flow. The
     * {@code HttpServerExchange} must not be modified by {@code nextProvider}. Any modifications, like setting headers
     * or even start sending responses are to be done by the {@code HttpHandler} emitted by the returned {@code Single}.
     * On the other hand, it's allowed to consume the request payload in order to start processing in parallel. Note
     * that the outcome of the authorization is not yet determined when {@code nextProvider} is invoked, so all actions
     * that may only be performed when the request is authorized are to be carried out when the emitted
     * {@code HttpHandler} is invoked.</p>
     *
     * @param   predicate     requirements that the HTTP request must fulfill in order to be authorized
     * @param   nextProvider  function that is called (potentially asynchronously) with the {@code HttpServerExchange}
     *                        to be served and that returns a {@code Single} that emits the {@code HttpHandler} to be
     *                        used to further process the request
     *
     * @return  a {@code HttpHandler} that, if authorized, delegates request processing to the {@literal "next"}
     *          handler, or, if unauthorized, to a {@code HttpHandler} that sends an appropriate error response to
     *          clients.
     *
     * @throws  NullPointerException  if at least one of the parameters is {@code null}
     */
    HttpHandler require(Predicate<? super AuthenticationInfo> predicate,
            Function<? super HttpServerExchange, ? extends Single<HttpHandler>> nextProvider);

}
