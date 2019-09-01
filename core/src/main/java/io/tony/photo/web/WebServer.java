package io.tony.photo.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger log = LoggerFactory.getLogger(WebServer.class);

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
    registryRequestHandler(router -> {
      router.route("/static/*").handler(StaticHandler.create());
      router.route("/").handler(ctx -> ctx.reroute("/static/index.html"));
    });
  }

  public void registryRequestHandler(RequestRegistry requestRegistry) {
    this.registries.add(requestRegistry);
  }

  public void start() {
    if (started.compareAndSet(false, true)) {
      this.registries.stream().forEach(registry -> registry.buildRequestRegistry(router));
      if (log.isDebugEnabled()) {
        router.getRoutes().forEach(route -> log.debug("Mapped {}", route.getPath()));
      }
      httpServer.requestHandler(router).listen(port);
      if (log.isDebugEnabled()) {
        log.debug("Http service started, listened on port: {}", port);
      }
    }
  }

  @Override
  public void close() throws IOException {
    vertx.close();
    httpServer.close();
  }
}
