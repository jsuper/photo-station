package io.tony.photo.web;

import io.vertx.ext.web.Router;

public interface RequestRegistry {

  void buildRequestRegistry(Router router);
}
