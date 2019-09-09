package io.tony.photo.web;


import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.tony.photo.exception.PhotoDuplicateException;
import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.utils.FileOp;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class PhotoRequestHandlerRegistry implements RequestRegistry {
  private static final Logger log = LoggerFactory.getLogger(PhotoRequestHandlerRegistry.class);
  private static final int PAGE_SIZE = 12;
  public static final String API_PHOTO_UPLOAD = "/api/photos/upload";
  private PhotoStore photoStore;
  private PhotoIndexStore photoIndexStore;

  private static final String[] AGG_FIELD = new String[]{"tags", "albums", "shoot_date"};

  private Path uploadDir;
  private Vertx vertx;

  public PhotoRequestHandlerRegistry(PhotoStore photoStorage) {
    this.photoStore = photoStorage;
    this.photoIndexStore = photoStorage.getIndexStore();
    this.uploadDir = Paths.get(System.getProperty("java.io.tmpdir"), "photos");
    if (Files.notExists(uploadDir)) {
      FileOp.createDirectoriesQuietly(this.uploadDir);
    }
  }

  @Override
  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void buildRequestRegistry(Router router) {
    router.route("/api/nav-agg").handler(this::navigationHandler);
    router.route("/api/photos").handler(this::handlePhotoLists);
    router.route("/api/photo/:photo").handler(this::handlePhotoImage);

    router.route(API_PHOTO_UPLOAD).handler(BodyHandler.create()
      .setMergeFormAttributes(true));
    router.route(API_PHOTO_UPLOAD).handler(this::handlePhotoUpload);
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
    String size = request.getParam("size");
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
      int pageSize = size == null || size.isBlank() ? PAGE_SIZE : Integer.parseInt(size);
      List<PhotoMetadata> data = photoIndexStore.list(from, pageSize, fq);
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

  private void handlePhotoUpload(RoutingContext ctx) {
    Set<FileUpload> fileUploads = ctx.fileUploads();
    int succeed = 0;
    Map<String, String> errorFile = new LinkedHashMap<>();
    if (fileUploads != null && !fileUploads.isEmpty()) {
      for (FileUpload fileUpload : fileUploads) {
        try {
          Buffer uploadedFile = vertx.fileSystem().readFileBlocking(fileUpload.uploadedFileName());
          String fileName = URLDecoder.decode(fileUpload.fileName(), "UTF-8");

          String id = DigestUtils.md5Hex(uploadedFile.getBytes());

          Path tempFile = this.uploadDir.resolve(id + "/" + fileName);
          Files.createDirectory(tempFile.getParent());

          try (OutputStream fos = Files.newOutputStream(tempFile)) {
            fos.write(uploadedFile.getBytes());
            photoStore.add(tempFile);
            System.out.println("success upload file: " + fileName);
            succeed++;
          } catch (Exception e) {
            if (e instanceof PhotoDuplicateException) {
              errorFile.put(fileName, "Photo already exists");
            } else {
              errorFile.put(fileName, "Upload file error");
            }
          } finally {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempFile.getParent());
          }

        } catch (Exception e) {
          log.error("Failed to process photo uploaded.", e);
          errorFile.put(fileUpload.fileName(), "Process error");
        }
      }
    }
    ctx.response().putHeader("Content-Type", "application/json");
    Map<String, Object> data = new LinkedHashMap<>();
    int totalFile = fileUploads.size();
    data.put("total", totalFile);
    data.put("succeed", succeed);
    data.put("errors", errorFile);
    if (totalFile == succeed) {
      data.put("status", 1);
      data.put("message", "Successfully uploaded all files.");
    } else {
      data.put("status", succeed == 0 ? 0 : 2);
      data.put("message", succeed == 0 ? "All files uploaded failed." : "Partial files uploaded failed.");
    }
    ctx.response().end(Json.toJson(data));
  }
}
