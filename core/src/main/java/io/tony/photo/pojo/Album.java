package io.tony.photo.pojo;

import java.util.Date;

import lombok.Data;

/**
 * 影集
 */
@Data
public class Album {

  private String id;
  private String name;
  private String cover;
  private Double coverRatio;
  private Date start;
  private Date end;
  private long photos;
  private long createTime;
}
