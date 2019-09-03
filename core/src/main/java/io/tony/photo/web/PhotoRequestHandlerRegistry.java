package io.tony.photo.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class PhotoRequestHandlerRegistry implements RequestRegistry {
  private static final int PAGE_SIZE = 12;
  private PhotoStore photoStore;
  private PhotoIndexStore photoIndexStore;

  private static final String[] AGG_FIELD = new String[]{"tags", "albums", "shoot_date"};

  public PhotoRequestHandlerRegistry(PhotoStore photoStorage) {
    this.photoStore = photoStorage;
    this.photoIndexStore = photoStorage.getIndexStore();
  }

  @Override
  public void buildRequestRegistry(Router router) {
    router.route("/api/nav-agg").handler(this::navigationHandler);
    router.route("/api/photos").handler(this::handlePhotoLists);
    router.route("/api/photo/:photo").handler(this::handlePhotoImage);
  }

  /**
   * 生成导航菜单
   */
  private void navigationHandler(RoutingContext ctx) {
    HttpServerRequest request = ctx.request();

    String aggFields = request.getParam("agg");
    String[] aggregation = null;
    if (aggFields == null || aggFields.isBlank()) {
      aggregation = AGG_FIELD;
    } else {
      aggregation = aggFields.split(",");
    }
    Map<String, Map<String, Long>> aggregate = this.photoIndexStore.aggregate(10, aggregation);
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().end(Json.toJson(aggregate));
  }

  /**
   * 处理照片流请求
   */
  private void handlePhotoLists(RoutingContext ctx) {
    HttpServerRequest request = ctx.request();
    String fromParam = request.getParam("from");
    Map<String, Object> fq = Collections.emptyMap();
    int from = 0;
    if (fromParam != null && !fromParam.isBlank()) {
      from = Integer.parseInt(fromParam);
    }
    String query = request.getParam("q");
    if (query != null && !query.isBlank()) {
      fq = Arrays.stream(query.split(",")).map(qf -> qf.split(":"))
        .collect(Collectors.toMap(qf -> qf[0], qf -> qf[1]));

    }

    try {
      List<PhotoMetadata> data = photoIndexStore.list(from, PAGE_SIZE, fq);
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
  }

  /**
   * 获取照片缩略图或者大图
   */
  private void handlePhotoImage(RoutingContext ctx) {
    String photoId = ctx.request().getParam("photo");
    boolean isThumbnail = "true".equals(ctx.request().getParam("t"));

    if (!Strings.isBlank(photoId)) {
      File imagePath = null;
      String type;
      if (isThumbnail) {
        imagePath = photoStore.getThumbnail(photoId).toFile();
        type = "jpg";
      } else {
        PhotoMetadata metadataFromDisk = photoStore.getMetadataFromDisk(photoId);
        type = metadataFromDisk.getType();
        if (metadataFromDisk != null && !Strings.isBlank(metadataFromDisk.getPath())) {
          try {
            System.out.println(metadataFromDisk.getPath()) ;
            imagePath = new File(metadataFromDisk.getPath());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

      if (imagePath != null && imagePath.exists()) {
        HttpServerResponse response = ctx.response();
        response.putHeader("Content-Type", "image/" + type);
        try {
          response.sendFile(imagePath.getCanonicalPath()).end();
        } catch (IOException e) {
          ctx.fail(500, e);
        }
      }
      return;
    }
    ctx.fail(404);
  }
}
