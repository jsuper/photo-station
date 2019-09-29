package io.tony.photo.web;

import io.tony.photo.utils.Json;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public interface RequestRegistry {

  void buildRequestRegistry(Router router);

  default void setVertx(Vertx vertx) {
    //do nothing
  }

  default void json(RoutingContext ctx, Object responseBody) {
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().end(Json.toJson(responseBody));
  }
}
