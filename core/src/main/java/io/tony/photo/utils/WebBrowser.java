package io.tony.photo.utils;

import java.util.concurrent.CountDownLatch;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import static io.vertx.core.Vertx.vertx;

public class WebBrowser {

  private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";
  private static WebClientOptions options = new WebClientOptions()
    .setUserAgent(UA);
  private static final WebClient client = WebClient.create(vertx(), options);

  public static String get(String uri) {
    CountDownLatch lock = new CountDownLatch(1);
    client.get(uri).send(ar ->{
      lock.countDown();
      String body = ar.result().bodyAsString();

    });
    try {
      lock.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return null;
  }
}
