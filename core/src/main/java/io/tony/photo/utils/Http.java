package io.tony.photo.utils;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Http {

  private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";

  public static InputStream getRaw(String url) throws Exception {

    URL actualUrl = new URL(url);
    HttpURLConnection huc = (HttpURLConnection) actualUrl.openConnection();
    huc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
    int responseCode = huc.getResponseCode();
    if (responseCode == 200) {
      return huc.getInputStream();
    } else if (responseCode == 302) {
      String location = huc.getHeaderField("Location");
      if (location != null) {
        return getRaw(location);
      }
    }
    return null;
  }

  public static String get(String url, Map<String, String> headers, boolean followRedirect) throws Exception {
    URL request = new URL(url);
    HttpURLConnection connection = (HttpURLConnection) request.openConnection();
    Map<String, String> header = headers;
    if (headers == null || !headers.containsKey("User-Agent")) {
      header = new HashMap<>(headers);
      header.put("User-Agent", UA);
    }
    header.forEach((k, v) -> connection.addRequestProperty(k, v));
    int responseCode = connection.getResponseCode();
    if (followRedirect && responseCode == 302) {
      return get(connection.getHeaderField("location"), headers, false);
    }

    Map<String, List<String>> headerFields = connection.getHeaderFields();
    System.out.println(headerFields);
    InputStream inputStream = connection.getInputStream();
    try {
      String body = IOUtils.toString(inputStream, "UTF-8");
      return body;
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public static String get(String url) {
    try (InputStream raw = getRaw(url)) {
      return IOUtils.toString(raw, "UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
