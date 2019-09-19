package io.tony.photo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.tony.photo.pojo.Location;
import io.tony.photo.service.LbsService;
import io.tony.photo.utils.Http;
import io.tony.photo.utils.Json;

/**
 * 腾讯地理位置信息服务，采用腾讯公共api
 */
public class TxLbsService implements LbsService {
  private static final Logger log = LoggerFactory.getLogger(TxLbsService.class);

  private static final String key = "OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77";
  private static final String referrer = "https://lbs.qq.com/webservice_v1/guide-gcoder.html";
  private static final String api = "https://apis.map.qq.com/ws/geocoder/v1/";

  private static final Map<String, String> BASE_HEADER;

  static {
    Map<String, String> header = new HashMap<>();
    header.put("Sec-Fetch-Mode", "no-cors");
    header.put("Referer", referrer);
    header.put("DNT", "1");
    BASE_HEADER = Collections.unmodifiableMap(header);
  }

  @Override
  public Optional<Location> getLocation(double latitude, double longitude) {
    StringBuilder request = new StringBuilder(api).append("?")
      .append("location=").append(latitude).append("%2C").append(longitude).append("&")
      .append("get_poi=0").append("&").append("key=").append(key)
      .append("&output=json&_=").append(System.currentTimeMillis());

    try {
      String locationResponse = Http.get(request.toString(), BASE_HEADER, false);
      return Optional.ofNullable(locationResponse)
        .filter(resp -> !resp.isBlank())
        .flatMap(location -> {
          Map<String, Object> jsonMap = Json.from(location, Map.class);
          if (jsonMap != null && jsonMap.containsKey("result")) {
            Map<String, Object> result = (Map<String, Object>) jsonMap.get("result");
            Location li = new Location();
            String address = (String) result.getOrDefault("address", "");
            Map<String, String> addressMap = (Map<String, String>) result.getOrDefault("address_component", Collections.emptyMap());
            String nation = addressMap.get("nation");
            String province = addressMap.get("province");
            String city = addressMap.get("city");
            String district = addressMap.get("district");
            String street = addressMap.get("street");

            li.setNation(nation);
            li.setProvince(province);
            li.setCity(city);
            li.setAddress(address);
            li.setDistrict(district);
            li.setStreet(street);
            return Optional.of(li);
          }
          return Optional.empty();
        });
    } catch (Exception e) {
      log.error("Get location from tx api failed.", e);
    }
    return Optional.empty();
  }
}
