package io.tony.photo.pojo;

import lombok.Data;

@Data
public class Camera {

  //制造商
  private String maker;
  //型号
  private String model;
  //光圈
  private String aperture;
  //快门
  private String shutter;
  //焦距
  private String focalLength;
  //感光度
  private String iso;
}
