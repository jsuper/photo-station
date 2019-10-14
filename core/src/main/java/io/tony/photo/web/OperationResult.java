package io.tony.photo.web;

public class OperationResult {

  private int code ;
  private String message ;

  public OperationResult(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
