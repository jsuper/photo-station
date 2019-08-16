package io.tony.photo.service;

import io.tony.photo.pojo.PhotoMetadata;

public interface PhotoListener {

  enum OpType {
    ADD, UPDATE, DELETE, META_CREATE;
  }

  void before(PhotoMetadata metadata, OpType type);

  void after(PhotoMetadata metadata, OpType type);
}
