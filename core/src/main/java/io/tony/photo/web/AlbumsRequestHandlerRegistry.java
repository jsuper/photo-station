package io.tony.photo.web;

import java.util.List;

import io.tony.photo.pojo.Album;
import io.tony.photo.service.AlbumStore;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class AlbumsRequestHandlerRegistry implements RequestRegistry {

  private AlbumStore albumStore;

  public AlbumsRequestHandlerRegistry(AlbumStore albumStore) {
    this.albumStore = albumStore;
  }

  @Override
  public void buildRequestRegistry(Router router) {

    router.route(HttpMethod.GET, "/api/album/:id").handler(ctx -> {
      String id = ctx.pathParam("id");
      if (Strings.notBlank(id)) {
        Album album = albumStore.getAlbum(id);
        json(ctx, album);
      } else {
        ctx.fail(404);
      }
    });
    router.route(HttpMethod.PUT, "/api/album/:name").handler(ctx -> {
      try {
        String albumName = ctx.pathParam("name");
        Album album = new Album();
        album.setCreateTime(System.currentTimeMillis());
        album.setPhotos(0);
        album.setName(albumName);
        albumStore.addAlbum(album);

        json(ctx, album);
      } catch (Exception e) {
        ctx.fail(500, e);
      }
    });

    router.route(HttpMethod.POST, "/api/album/:id").handler(ctx -> {
      try {
        String albumId = ctx.pathParam("id");
        String albumJson = ctx.getBodyAsString("UTF-8");
        Album album;
        if (albumId != null && albumJson != null && (album = Json.from(albumJson, Album.class)) != null) {
          Album old = albumStore.getAlbum(albumId);
          if (old == null) {
            ctx.fail(404);
          } else {
            old.setName(album.getName());
            album = albumStore.updateAlbum(old);
            json(ctx, album);
          }
        }
        ctx.fail(400);
      } catch (Exception e) {
        ctx.fail(500);
      }
    });

    router.route(HttpMethod.GET, "/api/albums").handler(ctx -> {
      int loadSize = Integer.MAX_VALUE;
      String size = ctx.request().getParam("size");
      if (size != null && !size.isEmpty()) {
        loadSize = Integer.parseInt(size);
      }
      json(ctx, albumStore.getAlbums(loadSize));
    });
    router.route(HttpMethod.POST, "/api/album/:id/photos").handler(BodyHandler.create());
    router.route(HttpMethod.POST, "/api/album/:id/photos").handler(this::handleAddPhotoToAlbum);
  }

  private void handleAddPhotoToAlbum(RoutingContext ctx) {
    OpResult result = null;
    try {
      String photoIdJson = ctx.getBodyAsString();
      List<String> photoId = Json.from(photoIdJson, List.class);
      String albumId = ctx.pathParam("id");
      if (Strings.notBlank(albumId) && photoId != null && photoId.size() > 0) {
        albumStore.addPhotos(albumId, photoId);
        result = new OpResult(200, "添加成功");
      } else {
        result = new OpResult(400, "请求参数错误");
      }
    } catch (Exception e) {
      result = new OpResult(500, "添加相册失败：" + e.getMessage());
    }
    json(ctx, result);
  }

}
