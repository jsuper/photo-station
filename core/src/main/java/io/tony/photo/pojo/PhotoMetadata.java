package io.tony.photo.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.Date;
import java.util.Set;

import lombok.Data;

/**
 * 照片元数据
 */
@Data
public class PhotoMetadata {

  public static final String UNKNOWN_DEVICE = "unknown";

  //照片存储路径
  private String path;

  //照片标签
  private Set<String> tags;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date shootingDate;

  //地理位置信息
  private LocationInfo locationInfo;
  private double latitude;
  private double longitude;

  //虚拟相册
  private Set<String> album;

  //拍摄设备名称
  private String device = UNKNOWN_DEVICE;

  //照片sha1
  private String id;

  //生成元数据的时间戳
  private long timestamp;

  //图片大小
  private long size;

  private String type;

}
