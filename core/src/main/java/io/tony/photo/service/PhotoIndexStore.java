package io.tony.photo.service;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import io.tony.photo.pojo.Photo;
import io.tony.photo.service.impl.agg.AggregateTerm;

public interface PhotoIndexStore extends Closeable {

  void index(Photo photoMetadata);

  void index(List<Photo> photoMetadata);

  List<String> list(int start, int page, Map<String, String> query);

  long total();

  Map<String,List<AggregateTerm>> aggregate(int topN, String... aggFieldName) ;

  void refreshSearcher() ;
}
