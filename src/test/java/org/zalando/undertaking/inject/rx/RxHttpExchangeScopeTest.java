package org.zalando.undertaking.inject.rx;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.zalando.undertaking.inject.HttpExchangeScopeInjectionTestBase;

import com.google.inject.Injector;
import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;

import io.reactivex.Observable;
import io.reactivex.Single;

import io.undertow.server.HttpServerExchange;

import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

public class RxHttpExchangeScopeTest extends HttpExchangeScopeInjectionTestBase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private Injector injector;
    private RxHttpExchangeScope underTest;

    @Before
    public void setUp() throws Exception {
        injector = getInjector();
        underTest = injector.getInstance(RxHttpExchangeScope.class);
    }

    @Test
    public void throwsWithoutScopedExcutionObservable() {
        getSimpleHandlerSingle().map(TestSimpleHandler::getHeaderMap) //
                                .test()                               //
                                .assertError(err ->                   //
                                         isExceptionWithCause(err, ProvisionException.class, //
                                            OutOfScopeException.class));
    }

    @Test
    public void scopesOnSuccessObservable() {
        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Observable.just(1)                                                 //
                  .lift(underTest.scoped(demoExchange))                    //
                  .flatMap((e) -> getSimpleHandlerSingle().toObservable()) //
                  .map(TestSimpleHandler::getHeaderMap)                    //
                  .test()                                                  //
                  .assertComplete()                                        //
                  .assertValue(headers ->                                  //
                           headers.getFirst("X-Some-Header").equals("blah"));

    }

    @Test
    public void scopesErrorObservable() {
        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Observable.error(new RuntimeException("trigger error")) //
                  .lift(underTest.scoped(demoExchange))         //
                  .onErrorResumeNext(throwable -> {             //

                      TestSimpleHandler testhandler =                                                        //
                          getNamedHandler(TestSimpleHandler.class, "testhandler", injector);                 //
                      return Observable.just(testhandler.getHeaderMap());                                    //
                  })                                                                                         //
                  .map((o) -> (HeaderMap) o)                                                                 //
                  .test()                                                                                    //
                  .assertComplete()                                                                          //
                  .assertValue(headers ->                                                                    //
                           headers.getFirst("X-Some-Header").equals("blah"));

    }

    @Test
    public void throwsWithoutScopedExcutionSingle() {
        getSimpleHandlerSingle().map(TestSimpleHandler::getHeaderMap) //
                                .test()                               //
                                .assertError(err ->                   //
                                         isExceptionWithCause(err, ProvisionException.class, //
                                            OutOfScopeException.class));
    }

    @Test
    public void scopesOnSuccessSingle() {
        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Single.just(1)                                    //
              .lift(underTest.scopedSingle(demoExchange)) //
              .flatMap((e) -> getSimpleHandlerSingle())   //
              .map(TestSimpleHandler::getHeaderMap)       //
              .test()                                     //
              .assertComplete()                           //
              .assertValue(headers ->                     //
                       headers.getFirst("X-Some-Header").equals("blah"));

    }

    @Test
    public void scopesErrorSingle() {
        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Single.error(new RuntimeException("trigger error")) //
              .lift(underTest.scopedSingle(demoExchange))   //
              .onErrorResumeNext(throwable -> {             //

                  TestSimpleHandler testhandler =                                                        //
                      getNamedHandler(TestSimpleHandler.class, "testhandler", injector);                 //
                  return Single.just(testhandler.getHeaderMap());                                        //
              })                                                                                         //
              .map((o) -> (HeaderMap) o)                                                                 //
              .test()                                                                                    //
              .assertComplete()                                                                          //
              .assertValue(headers ->                                                                    //
                       headers.getFirst("X-Some-Header").equals("blah"));

    }

    private Single<TestSimpleHandler> getSimpleHandlerSingle() {
        return Single.fromCallable(() -> getNamedHandler(TestSimpleHandler.class, "testhandler", injector));
    }

    private Boolean isExceptionWithCause(final Throwable err, final Class<? extends Throwable> throwable,
            final Class<? extends Throwable> cause) {
        return err.getClass().equals(throwable) && err.getCause().getClass().equals(cause);
    }
}
