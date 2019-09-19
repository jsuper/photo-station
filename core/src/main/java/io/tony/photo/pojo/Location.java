package io.tony.photo.pojo;

import lombok.Data;

/**
 * 地理信息
 */
@Data
public class Location {

  //国家
  private String nation;

  //省
  private String province;

  //市
  private String city;

  //地区 or 县
  private String district;

  //街道
  private String street;

  private String address;

}
