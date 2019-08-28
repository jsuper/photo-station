package io.tony.photo.service;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import io.tony.photo.pojo.PhotoMetadata;

public interface PhotoIndexStore extends Closeable {

  void index(PhotoMetadata photoMetadata);

  void index(List<PhotoMetadata> photoMetadata);

  void get(String photoId);

  List<PhotoMetadata> list(int start, int page, Map<String, Object> query);

  long total();
}
