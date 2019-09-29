package io.tony.photo.utils;

public class Strings {

  public static boolean isBlank(String value) {
    return value == null || value.length() == 0;
  }

  public static boolean notBlank(String value) {
    return !isBlank(value);
  }
}
