package io.tony.photo.service;

import io.tony.photo.pojo.Photo;

/**
 * Post handler for metadata
 */
public interface MetadataHandler {

  /**
   *
   */
  void handle(Photo metadata);
}
