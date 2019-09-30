package io.tony.photo.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class OpResult {

  private int code ;
  private String message ;
}
