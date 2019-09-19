package io.tony;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import io.tony.photo.service.impl.agg.AggregateTerm;
import io.tony.photo.service.impl.agg.Aggregation;
import io.tony.photo.service.impl.agg.AggregatorCache;

public class LuceneDemo {

  public static void main(String[] args) throws Exception {
    Path indexes = Paths.get("D:\\photos\\.index\\indexes");
    FSDirectory directory = FSDirectory.open(indexes);

    IndexReader reader = DirectoryReader.open(directory);
    SearcherManager sm = new SearcherManager(directory, new SearcherFactory());
    AggregatorCache ac = new AggregatorCache(sm);

    Map<String, Aggregation> aggregations = ac.getAggregations();
    aggregations.forEach((k, v) -> {
      System.out.println(k + ":");
      List<AggregateTerm> terms = v.terms();
      terms.forEach(t -> System.out.println("\t" + t.getValue() + ":" + t.getCounter()));
    });

  }
}
