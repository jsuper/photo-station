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
    webServer.registerHandler("/api/photos", "application/json", ctx -> {
      HttpServerRequest request = ctx.request();
      String fromParam = request.getParam("from");
      int from = 0 ;
      if(fromParam!=null && !fromParam.isBlank()) {
        from = Integer.parseInt(fromParam) ;
      }
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
    webServer.registerHandler("/api/photo/:photo", ctx -> {
      String photoId = ctx.request().getParam("photo");
      boolean isThumbnail ="true".equals( ctx.request().getParam("t"));

      if (!Strings.isBlank(photoId)) {
        File imagePath = null ;
        String type ;
        if (isThumbnail) {
          imagePath = photoStore.getThumbnail(photoId).toFile() ;
          type = "jpg" ;
        }else {
          PhotoMetadata metadataFromDisk = photoStore.getMetadataFromDisk(photoId);
          type = metadataFromDisk.getType() ;
          if (metadataFromDisk != null && !Strings.isBlank(metadataFromDisk.getPath())) {
            try {
              imagePath = new File(new URL(metadataFromDisk.getPath()).toURI());
            } catch (URISyntaxException | MalformedURLException e) {
              e.printStackTrace();
            }
          }
        }

        if(imagePath!=null && imagePath.exists()) {
          HttpServerResponse response = ctx.response();
          response.putHeader("Content-Type", "image/" + type);
          try {
            response.sendFile(imagePath.getCanonicalPath()).end();
          } catch (IOException e) {
            ctx.fail(500,e);
          }
        }
        return;
      }
      ctx.fail(404);
    });

    webServer.start();
  }

  public static void main(String[] args) throws Exception {
    new App("D:\\photos", 8889).run();
  }
}
