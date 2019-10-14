package io.tony.photo.service;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import io.tony.photo.pojo.Photo;

/**
 * Store photo
 */
public interface PhotoStore extends Closeable {

  boolean add(Path photo);

  boolean add(InputStream photoStream);

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

  Photo getMetadataFromDisk(String metadataId);

  /**
   * 刷新当前存储，重新生成照片元数据、重新索引。
   */
  void refresh();

  void refreshIndexesFromMetadata() ;

  Path getThumbnail(String photoId);

  PhotoIndexStore getIndexStore();

  Photo getPhotoById(String id) ;

  Photo update(Photo photo);

  //Flush metadata to disk
  void flush(Photo photo) ;
}
