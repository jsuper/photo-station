package io.tony.photo.service.impl.agg;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class AggregatorCache {
  private static final Logger log = LoggerFactory.getLogger(AggregatorCache.class);

  private static final String CACHE_KEY = "agg.all";
  private static final Map<String, Supplier<Aggregation>> aggregates;


  static {
    Map<String, Supplier<Aggregation>> aggs = new HashMap<>();
    aggs.put("albums", () -> new StringAggregation());
    aggs.put("tags", () -> new StringAggregation());
    aggs.put("date", () -> new StringAggregation(fullDate -> fullDate.substring(0, 4)));
    aggs.put("cam_maker", () -> new StringAggregation());

    aggregates = Collections.unmodifiableMap(aggs);
  }

  private SearcherManager searcherManager;
  private LoadingCache<String, Map<String, Aggregation>> aggCacheLoader;

  public AggregatorCache(SearcherManager searcherManager) {
    this.searcherManager = searcherManager;
    this.aggCacheLoader = CacheBuilder.newBuilder()
      .refreshAfterWrite(10, TimeUnit.MINUTES)
      .build(new CacheLoader<>() {
        @Override
        public Map<String, Aggregation> load(String key) throws Exception {
          if (CACHE_KEY.equals(key)) {
            return aggregateAll();
          }
          return Collections.emptyMap();
        }
      });
  }

  private Map<String, Aggregation> aggregateAll() {
    IndexSearcher searcher = null;
    try {
      long start = System.currentTimeMillis();
      searcher = searcherManager.acquire();
      Query query = new MatchAllDocsQuery();
      AggregatorCollector results = new AggregatorCollector();
      searcher.search(query, results);
      start = System.currentTimeMillis() - start;
      log.info("Finished aggregating {} documents, elapsed {} ms", searcher.getIndexReader().numDocs(), start);
      return results.aggregations;
    } catch (IOException e) {
      log.error("Do aggregator failed...", e);
    } finally {
      if (searcher != null) {
        try {
          searcherManager.release(searcher);
        } catch (IOException e) {
          log.error("Release search failed.", e);
        }
      }
    }
    return Collections.emptyMap();
  }

  public int getTotalDocs(String field, String value) {
    Aggregation aggregation = getAggregations().get(field);
    if (aggregation != null && aggregation.terms() != null) {
      return aggregation.total(value);
    }
    return 0;
  }

  public Map<String, Aggregation> getAggregations() {
    try {
      return Collections.unmodifiableMap(aggCacheLoader.get(CACHE_KEY));
    } catch (ExecutionException e) {
      return Collections.emptyMap();
    }
  }

  public void flushCache() {
    this.aggCacheLoader.refresh(CACHE_KEY);
  }


  private static class AggregatorCollector implements Collector {

    private Map<String, Aggregation> aggregations = new HashMap<>();

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
      return new LeafCollector() {
        @Override
        public void setScorer(Scorable scorer) throws IOException {

        }

        @Override
        public void collect(int doc) throws IOException {
          Set<String> aggregateFields = aggregates.keySet();
          Document docs = context.reader().document(doc, aggregateFields);
          for (String aggregate : aggregateFields) {
            aggregations.computeIfAbsent(aggregate, k -> aggregates.get(k).get());
            IndexableField[] fields = docs.getFields(aggregate);
            if (fields != null && fields.length > 0) {
              for (IndexableField field : fields) {
                aggregations.get(aggregate).aggregate(field);
              }
            }
          }
        }
      };
    }

    @Override
    public ScoreMode scoreMode() {
      return ScoreMode.COMPLETE_NO_SCORES;
    }
  }
}
