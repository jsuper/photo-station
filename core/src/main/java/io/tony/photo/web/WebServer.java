package io.tony.photo.web;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class WebServer implements Closeable {

  private HttpServer httpServer;
  private Router router;
  private int port;
  private AtomicBoolean started = new AtomicBoolean(false);

  private Vertx vertx;
  private Set<RequestRegistry> registries = new HashSet<>();

  public WebServer(int port) {
    System.setProperty("vertx.disableDnsResolve", "true");
    vertx = Vertx.vertx();
    httpServer = vertx.createHttpServer();
    router = Router.router(vertx);
    this.port = port;
  }

  public void registryRequestHandler(RequestRegistry requestRegistry) {
    this.registries.add(requestRegistry);
  }

  public void registerHandler(String route, Handler<RoutingContext> handler) {
    router.route(route).handler(handler);
  }

  public void registerHandler(String route, String produce, Handler<RoutingContext> handler) {
    router.route(route).produces(produce).handler(handler);
  }

  public void start() {
    if (started.compareAndSet(false, true)) {


      httpServer.requestHandler(router).listen(port);
    }
  }

  @Override
  public void close() throws IOException {
    vertx.close();
    httpServer.close();
  }
}
