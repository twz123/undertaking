package org.zalando.undertaking.test.rx.hamcrest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import static org.hamcrest.StringDescription.asString;

import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;

import java.util.List;

import org.hamcrest.Matcher;

import org.hobsoft.hamcrest.compose.ComposeMatchers;

import rx.observers.TestSubscriber;

public final class TestSubscriberMatchers {

    public static <T> Matcher<TestSubscriber<T>> hasValues(final Matcher<? super List<T>> eventsMatcher) {
        return hasFeature("has onNext events matching", TestSubscriber::getOnNextEvents, eventsMatcher);
    }

    public static <T> Matcher<TestSubscriber<T>> hasOnlyValues(final Matcher<? super List<T>> valuesMatcher) {
        return describedAs("has only onNext events matching %0",
                ComposeMatchers.<TestSubscriber<T>>compose(hasNoErrors()).and(hasValues(valuesMatcher)),
                asString(valuesMatcher));
    }

    public static <T> Matcher<TestSubscriber<T>> hasOnlyValue(final Matcher<? super T> valueMatcher) {
        return describedAs("has only a single onNext event matching %0", hasOnlyValues(contains(valueMatcher)),
                asString(valueMatcher));
    }

    public static <T> Matcher<TestSubscriber<T>> hasNoValues() {
        return describedAs("has no onNext events", hasValues(is(empty())));
    }

    public static <T> Matcher<TestSubscriber<T>> hasErrors(final Matcher<? super List<Throwable>> errorsMatcher) {
        return hasFeature("has onError events matching", TestSubscriber::getOnErrorEvents, errorsMatcher);
    }

    public static <T> Matcher<TestSubscriber<T>> hasNoErrors() {
        return describedAs("has no onError events", hasErrors(is(empty())));
    }

    public static <T> Matcher<TestSubscriber<T>> hasOnlyErrors(final Matcher<? super List<Throwable>> errorsMatcher) {
        return describedAs("has only onError events matching %0",
                ComposeMatchers.<TestSubscriber<T>>compose(hasNoValues()).and(hasErrors(errorsMatcher)),
                asString(errorsMatcher));
    }

    public static <T> Matcher<TestSubscriber<T>> hasOnlyError(final Matcher<? super Throwable> errorMatcher) {
        return describedAs("has only a single onError event matching %0", hasOnlyErrors(contains(errorMatcher)),
                asString(errorMatcher));
    }

    private TestSubscriberMatchers() {
        throw new AssertionError("No instances for you!");
    }
}
