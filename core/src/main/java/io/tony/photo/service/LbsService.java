package io.tony.photo.service;

import java.util.Optional;

import io.tony.photo.pojo.LocationInfo;

/**
 * 地理位置服务
 */
public interface LbsService {

  /**
   * 根据经纬度获取地理位置信息
   *
   * @param latitude  纬度十进制
   * @param longitude 经度十进制
   * @return 地理位置信息
   */
  Optional<LocationInfo> getLocation(double latitude, double longitude);
}
