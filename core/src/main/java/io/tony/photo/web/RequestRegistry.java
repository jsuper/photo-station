package io.tony.photo.web;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public interface RequestRegistry {

  void buildRequestRegistry(Router router);

  default void setVertx(Vertx vertx) {
    //do nothing
  }
}
