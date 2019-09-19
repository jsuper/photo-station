package io.tony.photo.service;

import io.tony.photo.pojo.Photo;
import lombok.Getter;

/**
 * Photo changed listener
 */
public interface PhotoChangedListener {

  void onChanged(PhotoChangedEvent event);

  enum EventType {
    ADD, REMOVE, UPDATE, META_CREATED
  }

  @Getter
  class PhotoChangedEvent {

    private Photo photo;

    private EventType eventType;

    public PhotoChangedEvent(Photo photo, EventType eventType) {
      this.photo = photo;
      this.eventType = eventType;
    }

  }
}
