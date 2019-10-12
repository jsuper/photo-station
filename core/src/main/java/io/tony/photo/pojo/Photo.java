package io.tony.photo.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.Set;

import lombok.Data;

/**
 * 照片元数据
 */
@Data
public class Photo {

  public static final String UNKNOWN_DEVICE = "unknown";

  //照片sha1
  private String id;

  private String name;

  private String title;

  private String note;

  //照片存储路径
  private String path;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date date;

  private double latitude;
  private double longitude;
  //地理位置信息
  private Location location;

  //照片标签
  private Set<String> tags;

  //虚拟相册
  private Set<String> albums;

  //生成元数据的时间戳
  private long timestamp;

  //图片大小
  private long size;


  private String type;

  private int width;

  private int height;

  private Camera camera;

  private int favorite;

  private int deleted ;

  @JsonIgnore
  public Camera getOrCreateCamera() {
    if (this.camera == null) {
      synchronized (this) {
        if (this.camera == null) {
          this.camera = new Camera();
        }
      }
    }
    return camera;
  }

}
