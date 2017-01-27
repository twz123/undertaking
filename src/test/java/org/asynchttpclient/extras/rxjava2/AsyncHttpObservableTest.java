package org.asynchttpclient.extras.rxjava2;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;

import org.junit.Test;

import io.reactivex.Observable;

public class AsyncHttpObservableTest {

    @Test
    public void testToObservableNoError() {
        try(AsyncHttpClient client = asyncHttpClient()) {
            AsyncHttpObservable.toObservable(() -> client.prepareGet("http://gatling.io")).test()
                               .awaitDone(5, TimeUnit.SECONDS).assertValue(resp ->
                                       resp.getStatusCode() == 200);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testToObservableError() {
        try(AsyncHttpClient client = asyncHttpClient()) {
            AsyncHttpObservable.toObservable(() -> client.prepareGet("http://gatling.io/ttfn")).test()
                               .awaitDone(5, TimeUnit.SECONDS).assertValue(resp ->
                                       resp.getStatusCode() == 404);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testObserveNoError() {
        try(AsyncHttpClient client = asyncHttpClient()) {
            AsyncHttpObservable.observe(() -> client.prepareGet("http://gatling.io/")).test()
                               .awaitDone(5, TimeUnit.SECONDS).assertValue(resp ->
                                       resp.getStatusCode() == 200);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testObserveError() {
        try(AsyncHttpClient client = asyncHttpClient()) {
            AsyncHttpObservable.observe(() -> client.prepareGet("http://gatling.io/ttfn")).test()
                               .awaitDone(5, TimeUnit.SECONDS).assertValue(resp ->
                                       resp.getStatusCode() == 404);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testObserveMultiple() {
        try(AsyncHttpClient client = asyncHttpClient()) {
            Observable.merge(AsyncHttpObservable.observe(() -> client.prepareGet("http://gatling.io")),
                          AsyncHttpObservable.observe(() ->
                                  client.prepareGet("http://www.wisc.edu").setFollowRedirect(true)),
                          AsyncHttpObservable.observe(() ->
                                  client.prepareGet("http://www.umn.edu").setFollowRedirect(true))).test()
                      .awaitDone(5, TimeUnit.SECONDS).assertValueAt(0, resp -> resp.getStatusCode() == 200)
                      .assertValueAt(1, resp -> resp.getStatusCode() == 200).assertValueAt(2,
                          resp -> resp.getStatusCode() == 200);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}
