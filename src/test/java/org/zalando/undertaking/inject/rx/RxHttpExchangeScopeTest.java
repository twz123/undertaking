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

    @Before
    public void setUp() throws Exception {
        injector = getInjector();
    }

    @Test
    public void throwsWithoutScopedExcutionObservable() {
        Injector injector = getInjector();

        getSimpleHandlerSingle(injector).map(TestSimpleHandler::getHeaderMap)                     //
                                        .test()                                                   //
                                        .assertError(err ->                                       //
                                                 isExceptionWithCause(err, ProvisionException.class,
                                                    OutOfScopeException.class));                  //
    }

    @Test
    public void scopesOnSuccessObservable() {
        Injector injector = getInjector();
        RxHttpExchangeScope scope = injector.getInstance(RxHttpExchangeScope.class);

        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Observable.just(1)                                                         //
                  .lift(scope.scoped(demoExchange))                                //
                  .flatMap((e) -> getSimpleHandlerSingle(injector).toObservable()) //
                  .map(TestSimpleHandler::getHeaderMap)                            //
                  .test()                                                          //
                  .assertComplete()                                                //
                  .assertValue(headers ->                                          //
                           headers.getFirst("X-Some-Header").equals("blah"));      //

    }

    @Test
    public void scopesErrorObservable() {
        RxHttpExchangeScope scope = injector.getInstance(RxHttpExchangeScope.class);

        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Observable.error(new RuntimeException("trigger error")) //
                  .lift(scope.scoped(demoExchange))             //
                  .onErrorResumeNext(throwable -> {             //

                      TestSimpleHandler testhandler =                                                        //
                          getNamedHandler(TestSimpleHandler.class, "testhandler", injector);                 //
                      return Observable.just(testhandler.getHeaderMap());                                    //
                  })                                                                                         //
                  .map((o) -> (HeaderMap) o)                                                                 //
                  .test()                                                                                    //
                  .assertComplete()                                                                          //
                  .assertValue(headers ->                                                                    //
                           headers.getFirst("X-Some-Header").equals("blah"));                                //

    }

    @Test
    public void throwsWithoutScopedExcutionSingle() {
        Injector injector = getInjector();

        getSimpleHandlerSingle(injector).map(TestSimpleHandler::getHeaderMap)                     //
                                        .test()                                                   //
                                        .assertError(err ->                                       //
                                                 isExceptionWithCause(err, ProvisionException.class,
                                                    OutOfScopeException.class));                  //
    }

    @Test
    public void scopesOnSuccessSingle() {
        Injector injector = getInjector();
        RxHttpExchangeScope scope = injector.getInstance(RxHttpExchangeScope.class);

        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Single.just(1)                                              //
              .lift(scope.scopedSingle(demoExchange))               //
              .flatMap((e) -> getSimpleHandlerSingle(injector))     //
              .map(TestSimpleHandler::getHeaderMap)                 //
              .test()                                               //
              .assertComplete()                                     //
              .assertValue(headers ->                               //
                       headers.getFirst("X-Some-Header").equals("blah")); //

    }

    @Test
    public void scopesErrorSingle() {
        RxHttpExchangeScope scope = injector.getInstance(RxHttpExchangeScope.class);

        HttpServerExchange demoExchange = new HttpServerExchange(null);
        demoExchange.getRequestHeaders().put(HttpString.tryFromString("X-Some-Header"), "blah");

        Single.error(new RuntimeException("trigger error")) //
              .lift(scope.scopedSingle(demoExchange))       //
              .onErrorResumeNext(throwable -> {             //

                  TestSimpleHandler testhandler =                                                        //
                      getNamedHandler(TestSimpleHandler.class, "testhandler", injector);                 //
                  return Single.just(testhandler.getHeaderMap());                                        //
              })                                                                                         //
              .map((o) -> (HeaderMap) o)                                                                 //
              .test()                                                                                    //
              .assertComplete()                                                                          //
              .assertValue(headers ->                                                                    //
                       headers.getFirst("X-Some-Header").equals("blah"));                                //

    }

    private Single<TestSimpleHandler> getSimpleHandlerSingle(final Injector injector) {
        return Single.fromCallable(() -> getNamedHandler(TestSimpleHandler.class, "testhandler", injector));
    }

    private Boolean isExceptionWithCause(final Throwable err, final Class<? extends Throwable> throwable,
            final Class<? extends Throwable> cause) {
        return err.getClass().equals(throwable) && err.getCause().getClass().equals(cause);
    }
}
