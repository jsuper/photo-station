package io.tony.photo;

import com.drew.lang.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.service.impl.PhotoStoreImpl;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;
import io.tony.photo.web.WebServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.handler.StaticHandler;

public class App {

  private static final Logger log = LoggerFactory.getLogger(App.class);

  private static final int pageSize = 10;
  private PhotoStore photoStore;
  private WebServer webServer;

  private int port;
  private String path;

  public App(String path, int port) {
    this.path = path;
    this.port = port;
    photoStore = new PhotoStoreImpl(path);
    webServer = new WebServer(port);
  }

  public void run() {

    log.info("Running photo station on port: {}, folder: {}", port, path);
    final PhotoIndexStore pis = ((PhotoStoreImpl) photoStore).getIndexStore();

    webServer.registerHandler("/static/*", StaticHandler.create());
    webServer.registerHandler("/", ctx -> ctx.reroute("/static/index.html"));

    webServer.registerHandler("/api/photo/:photo", ctx -> {

    });

    webServer.start();
  }

  public static void main(String[] args) throws Exception {
    if (args == null || args.length < 1) {
      return;
    }
    String storage = args[0];
    int port = args.length >= 2 ? Integer.parseInt(args[1]) : 6666;
    new App(storage, port).run();
  }
}
