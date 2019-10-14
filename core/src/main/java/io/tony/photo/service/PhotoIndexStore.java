package io.tony.photo.service;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import java.io.Closeable;
import java.util.List;
import java.util.Map;


import io.tony.photo.pojo.Photo;
import io.tony.photo.service.impl.agg.AggregateTerm;

public interface PhotoIndexStore extends Closeable {

  void index(Photo photoMetadata);

  void index(List<Photo> photoMetadata);

  List<String> list(int start, int page, Map<String, String> query);

  List<String> list(int start, int page, String query);

  long total();

  Map<String,List<AggregateTerm>> aggregate(int topN, String... aggFieldName) ;

  void refreshSearcher() ;

  List<Document> search(Query query, SortField sort, int size) ;

  int deleteAlbums(String albumId);

  int getTotalDocs(String field, String value) ;
}
