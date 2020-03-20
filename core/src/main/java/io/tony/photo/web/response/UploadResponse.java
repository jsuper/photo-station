package io.tony.photo.web.response;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * @author Tang Ling
 * @version 1.0.0
 * @date 2020/3/20
 */
@Data
@Builder
public class UploadResponse {

  private Integer status;
  private String message;
  private Integer total;
  private Integer succeed;
  private Map<String, String> errors;
}
