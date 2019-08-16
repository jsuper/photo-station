package io.tony.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.service.impl.PhotoStoreImpl;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;
import io.tony.photo.web.WebServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

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

    webServer.registerHandler("/api/photos", "application/json", ctx -> {
      HttpServerRequest request = ctx.request();
      String page = request.getParam("page");
      int pageNo;
      if (page == null || !page.matches("\\d+")) {
        pageNo = 0;
      } else {
        pageNo = Integer.parseInt(page);
      }
      int from = pageNo * pageSize;
      System.out.println(pis == null);

      try {
        List<PhotoMetadata> data = pis.list(from, pageSize, Collections.emptyMap());
        data.forEach(meta -> {
          String path = "//" + request.host() + "/photo/" + meta.getId();
          meta.setPath(path);
        });

        HttpServerResponse response = ctx.response();
        response.putHeader("content-type", "application/json");
        String chunk = Json.toJson(data);
        response.end(chunk);
      } catch (Exception e) {
        e.printStackTrace();
        ctx.response().end(e.getMessage());
      }


    });
    webServer.registerHandler("/photo/:photo", ctx -> {
      String photoId = ctx.request().getParam("photo");
      if (!Strings.isBlank(photoId)) {
        PhotoMetadata metadataFromDisk = photoStore.getMetadataFromDisk(photoId);
        if (metadataFromDisk != null && !Strings.isBlank(metadataFromDisk.getPath())) {
          try {
            File filePath = new File(new URL(metadataFromDisk.getPath()).toURI());
            HttpServerResponse response = ctx.response();
            response.putHeader("Content-Type", "image/" + metadataFromDisk.getType());
            response.sendFile(filePath.getCanonicalPath()).end();
          } catch (Exception e) {
            e.printStackTrace();
            ctx.fail(500, e);
          }
          return;
        }
      }
      ctx.fail(404);
    });

    webServer.start();
  }

  public static void main(String[] args) throws Exception {
    new App("D:\\photos",8888).run();
  }
}
