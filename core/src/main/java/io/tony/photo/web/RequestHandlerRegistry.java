package io.tony.photo.web;

import io.vertx.ext.web.Router;

public class RequestHandlerRegistry {

  private Router router;

  public RequestHandlerRegistry(Router router) {
    this.router = router;
  }

  public void buildRegistry() {

  }
}
