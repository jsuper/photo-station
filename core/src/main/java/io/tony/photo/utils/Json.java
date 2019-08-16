package io.tony.photo.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Json {

  static ObjectMapper engine = new ObjectMapper();

  public static final String toJson(Object object) {
    try {
      return engine.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize to json string.", e);
    }
  }

  public static final <T> T from(String json, Class<T> clazz) {
    try {
      return engine.readValue(json, clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid json doc: " + json, e);
    }
  }

  public static <T> T from(InputStream stream, Class<T> clazz) {
    try {
      return engine.readValue(stream, clazz);
    } catch (IOException e) {
      throw new IllegalStateException("De-serialize from stream failed.", e);
    }
  }

  public static <T> T from(Path file, Class<T> clazz) {
    if (Files.notExists(file)) {
      throw new IllegalStateException("File not found: " + file);
    }
    try {
      return engine.readValue(file.toFile(), clazz);
    } catch (IOException e) {
      throw new IllegalStateException("De-serialize from file <" + file + "> failed.", e);
    }
  }
}
