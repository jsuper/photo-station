package io.tony.photo.exception;

/**
 * Exception which indicate the photo was duplicate
 */
public class PhotoDuplicateException extends RuntimeException {

  public PhotoDuplicateException(String message) {
    super(message);
  }
}
