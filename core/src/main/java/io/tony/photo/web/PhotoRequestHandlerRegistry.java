package io.tony.photo.web;


import com.google.common.collect.ImmutableMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.tony.photo.exception.PhotoDuplicateException;
import io.tony.photo.pojo.Photo;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.utils.FileOp;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
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

  private static final String[] AGG_FIELD = new String[]{"tags", "albums", "date"};

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
    router.route(HttpMethod.GET, "/api/photo/:photo").handler(this::handlePhotoImage);
    router.route(HttpMethod.PUT, "/api/photo/:photo").handler(this::handleUpdatePhotoMeta);
    router.route(HttpMethod.POST, "/api/photos/upload").handler(this::handlePhotoUpload);
    router.route(HttpMethod.PUT, "/api/trash/photos").handler(this::handleDeletePhoto);
    router.route(HttpMethod.PUT, "/api/favorite/photos").handler(this::handlePartialUpdate);
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
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().end(Json.toJson(this.photoIndexStore.aggregate(10, aggregation)));
  }

  /**
   * 处理照片流请求
   */
  private void handlePhotoLists(RoutingContext ctx) {
    HttpServerRequest request = ctx.request();
    String fromParam = request.getParam("from");
    String size = request.getParam("size");
    Map<String, String> fq = Collections.emptyMap();
    int from = 0;
    if (fromParam != null && !fromParam.isBlank()) {
      from = Integer.parseInt(fromParam);
    }
    String query = request.getParam("q");
    if (query != null && !query.isBlank()) {
      fq = Arrays.stream(query.split(",")).map(qf -> qf.split(":"))
        .collect(Collectors.toMap(qf -> "date".equals(qf[0]) ? "year" : qf[0], qf -> qf[1]));
    }

    try {
      int pageSize = size == null || size.isBlank() ? PAGE_SIZE : Integer.parseInt(size);
      List<String> data = photoIndexStore.list(from, pageSize, fq);
      List<Photo> photos = data.parallelStream().map(pid -> photoStore.getPhotoById(pid))
        .filter(Objects::nonNull).collect(Collectors.toList());
      HttpServerResponse response = ctx.response();
      response.putHeader("content-type", "application/json");
      String chunk = Json.toJson(photos);
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
        Photo metadataFromDisk = photoStore.getMetadataFromDisk(photoId);
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

  /**
   * 更新照片元数据
   */
  private void handleUpdatePhotoMeta(RoutingContext ctx) {
    int status = 0;
    String message;
    try {
      String photoUpdated = ctx.getBodyAsString();
      Photo from = Json.from(photoUpdated, Photo.class);
      Photo update = photoStore.update(from);
      log.info("Got updated: {}", update);
      message = "Success updated";
    } catch (Exception e) {
      status = 1;
      message = "Update error";
    }
    ctx.response().end(Json.toJson(ImmutableMap.of("status", status, "message", message)));
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
          if (Files.notExists(tempFile.getParent())) {
            Files.createDirectories(tempFile.getParent());
          }

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
    if (succeed > 0) {
      photoIndexStore.refreshSearcher();
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

  private void handleDeletePhoto(RoutingContext ctx) {
    int status;
    String message;
    try {
      String deletedPhotos = ctx.getBodyAsString();
      List<String> deleted;
      if (Strings.notBlank(deletedPhotos) && ((deleted = Json.from(deletedPhotos, List.class))) != null) {
        List<Photo> removed = deleted.parallelStream().map(id -> {
          try {
            Photo metadataFromDisk = photoStore.getMetadataFromDisk(id);
            metadataFromDisk.setDeleted(1);
            photoStore.flush(metadataFromDisk);
            return metadataFromDisk;
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (removed.size() > 0) {
          photoIndexStore.index(removed);
          photoIndexStore.refreshSearcher();
        }
        status = 200;
        message = "共" + deleted.size() + "条记录，删除" + removed.size() + "条记录";
      } else {
        status = 400;
        message = "请求参数不能为空";
      }
    } catch (Exception e) {
      status = 500;
      message = "照片移除失败";
    }
    ctx.response().end(Json.toJson(new OperationResult(status, message)));
  }

  private void handlePartialUpdate(RoutingContext ctx) {
    int status;
    String message;
    try {
      String json = ctx.getBodyAsString();
      PartialUpdateRequest req = Json.from(json, PartialUpdateRequest.class);
      if (req == null || req.getPhotos() == null || req.getPhotos().isEmpty() || req.getFields() == null ||
        req.getFields().isEmpty()) {
        status = 400;
        message = "请求参数错误";
      } else {
        List<Photo> updated = req.getPhotos().parallelStream().map(pid -> {
          try {
            Photo photo = photoStore.getMetadataFromDisk(pid);
            req.applyUpdate(photo);
            photoStore.flush(photo);
            return photo;
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!updated.isEmpty()) {
          this.photoIndexStore.index(updated);
          this.photoIndexStore.refreshSearcher();
        }
        status = 200;
        message = String.format("共%d条记录, 更新了%d条记录.", req.getPhotos().size(), updated.size());
      }
    } catch (Exception e) {
      status = 500;
      message = "更新失败";
    }
    ctx.response().end(Json.toJson(new OperationResult(status, message)));
  }
}
