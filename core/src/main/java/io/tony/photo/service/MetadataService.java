package io.tony.photo.service;

import java.nio.file.Path;

import io.tony.photo.pojo.PhotoMetadata;

/**
 * 照片元数据服务接口，主要负责读取照片的元数据信息
 */
public interface MetadataService {

  /**
   * 读取照片Exif元数据
   */
  PhotoMetadata readMetadata(Path photo);
}
