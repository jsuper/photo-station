package io.tony.photo.service;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import io.tony.photo.pojo.PhotoMetadata;

/**
 * Store photo
 */
public interface PhotoStore extends Closeable {

  boolean add(Path photo);

  boolean add(List<Path> photos);

  boolean remove(String id);

  boolean remove(Path path);

  /**
   * 为照片添加标签
   *
   * @param id   照片id
   * @param tags 需要添加的标签
   */
  Set<String> addTag(String id, String... tags);

  /**
   * 从给定目录导入照片
   *
   * @param sourceDirectory 照片存储路径
   */
  void importPhoto(Path sourceDirectory);

  PhotoMetadata getMetadataFromDisk(String metadataId);

}
