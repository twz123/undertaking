package org.zalando.undertaking.inject;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Scope;

import io.undertow.server.HttpServerExchange;

/**
 * Indicates that a resource is unique within a specific {@link HttpServerExchange}. Comparable to the servlet request
 * scope.
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface HttpExchangeScoped {
    // no methods to add
}
