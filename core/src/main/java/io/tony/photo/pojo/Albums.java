package io.tony.photo.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

import lombok.Data;

@Data
public class Albums {

  private String name;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date start;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date end;

  private String cover;
  private long photos;
}
