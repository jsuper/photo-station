package io.tony.photo.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.tony.photo.pojo.Photo;
import lombok.Data;

@Data
public class PartialUpdateRequest {
  private static Map<String, BiConsumer<Photo, String>> fieldApplier = new HashMap<>();

  static {
    fieldApplier.put("deleted", (p, v) -> p.setDeleted(Integer.parseInt(v)));
    fieldApplier.put("favorite", (p, v) -> p.setFavorite(Integer.parseInt(v)));
  }

  private List<String> photos;
  private Map<String, String> fields;


  void applyUpdate(Photo photo) {
    fields.forEach((k, v) -> {
      if (fieldApplier.containsKey(k)) {
        fieldApplier.get(k).accept(photo, v);
      }
    });
  }
}
