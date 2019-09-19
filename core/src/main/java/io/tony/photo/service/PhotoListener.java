package io.tony.photo.service;

import io.tony.photo.pojo.Photo;

public interface PhotoListener {

  enum OpType {
    ADD, UPDATE, DELETE, META_CREATE;
  }

  void before(Photo metadata, OpType type);

  void after(Photo metadata, OpType type);
}
