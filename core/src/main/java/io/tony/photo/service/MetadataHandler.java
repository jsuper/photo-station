package io.tony.photo.service;

import io.tony.photo.pojo.PhotoMetadata;

/**
 * Post handler for metadata
 */
public interface MetadataHandler {

  /**
   *
   */
  void handle(PhotoMetadata metadata);
}
