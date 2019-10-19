package io.tony.photo.service.impl;

import com.drew.lang.StringUtil;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.surround.parser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.tony.photo.pojo.Photo;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.impl.agg.AggregateTerm;
import io.tony.photo.service.impl.agg.Aggregation;
import io.tony.photo.service.impl.agg.AggregatorCache;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;

import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

public class LucenePhotoIndexStore implements PhotoIndexStore {
  private static final Logger log = LoggerFactory.getLogger(LucenePhotoIndexStore.class);

  static final Map<String, BiFunction<String, String, Query>> fieldQueries;

  static {
    Map<String, BiFunction<String, String, Query>> temp = new HashMap<>();
    temp.put("year", (field, val) -> IntPoint.newExactQuery(field, Integer.parseInt(val)));
    temp.put("favorite", (field, val) -> IntPoint.newExactQuery(field, Integer.parseInt(val)));
    fieldQueries = Collections.unmodifiableMap(temp);
  }

  static final BiFunction<String, String, Query> stringTermQuery = (field, val) -> new TermQuery(new Term(field, val));

  private Path indexDataFolder;
  private IndexWriterConfig.OpenMode openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND;

  private IndexWriter writer;
  private IndexReader reader;
  private SearcherManager searcherManager;


  private AtomicReference<IndexSearcher> searcherHolder = new AtomicReference<>();
  private AtomicInteger indexer = new AtomicInteger(0);

  private ScheduledExecutorService scheduledExecutorService;
  private AggregatorCache aggregator;

  public LucenePhotoIndexStore(Path indexFolder) {
    this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    this.scheduledExecutorService.scheduleAtFixedRate(() -> {
      if (indexer.get() > 0) {
        try {
          log.info("Refresh index reader from scheduler...");
          searcherManager.maybeRefresh();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          indexer.set(0);
        }
      }
    }, 0, 3, TimeUnit.MINUTES);
    this.indexDataFolder = indexFolder;

    Path indexes = this.indexDataFolder.resolve("indexes");

    try {
      IndexWriterConfig iwc = new IndexWriterConfig();
      iwc.setOpenMode(openMode);

      FSDirectory storeFolder = FSDirectory.open(indexes);
      this.writer = new IndexWriter(storeFolder, iwc);
      this.reader = DirectoryReader.open(writer);
      this.searcherManager = new SearcherManager(writer, new SearcherFactory());
      this.searcherHolder.compareAndSet(null, searcherManager.acquire());
      this.aggregator = new AggregatorCache(searcherManager);
    } catch (IOException e) {
      throw new IllegalStateException("Create internal searcher and reader failed.", e);
    }
  }

  @Override
  public void index(Photo photoMetadata) {
    index(Arrays.asList(photoMetadata));
  }

  @Override
  public void index(List<Photo> photoMetadata) {
    if (photoMetadata != null && !photoMetadata.isEmpty()) {
      try {
        photoMetadata.stream().forEach(meta -> {
          try {
            Document document = toDocument(meta);
            this.writer.updateDocument(new Term("id", meta.getId()), document);
          } catch (IOException e) {
            throw new IllegalStateException("Index document failed.", e);
          }
        });
        writer.commit();
        int currentUpdated = indexer.incrementAndGet();
        if (currentUpdated > 10) {
          this.searcherManager.maybeRefresh();
          indexer.set(0);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Index failed.", e);
      }
    }
  }

  private List<String> internalQuery(int from, int size, Query q) {
    final IndexSearcher searcher = getSearcher();
    int numDocs = from + size;
    int totalDocs = searcher.getIndexReader().numDocs();
    if (from > totalDocs) {
      return Collections.emptyList();
    }

    int numHint = Math.max(1, Math.min(numDocs, totalDocs));
    Sort sort = new Sort(new SortField("timestamp", SortField.Type.LONG, true));

    TopFieldCollector collector = TopFieldCollector.create(sort, numHint, null, 10000);

    try {
      if (searcher != null) {
        searcher.search(q, collector);
        TopDocs pageDocs = collector.topDocs(from, size);
        return Optional.ofNullable(pageDocs.scoreDocs)
          .map(doc ->
            Stream.of(doc).map(scoreDoc -> {
              try {
                return searcher.doc(scoreDoc.doc);
              } catch (IOException e) {
                return null;
              }
            }).filter(Objects::nonNull)
              .map(id -> id.getField("id").stringValue())
              .collect(Collectors.toList())).orElse(Collections.emptyList());
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      release(searcher);
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> list(int from, int size, Map<String, String> query) {
    Query imageQuery;

    if (query == null || query.isEmpty()) {
      imageQuery = new MatchAllDocsQuery();
    } else {
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      query.forEach((k, v) -> builder.add(fieldQueries.getOrDefault(k, stringTermQuery).apply(k, v), MUST));
      imageQuery = builder.build();
    }
    return internalQuery(from, size, imageQuery);
  }

  @Override
  public List<String> list(int start, int page, String query) {
    Query q;
    if (Strings.notBlank(query)) {
      String[] queries = query.split(",");
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      QueryParser qp = new QueryParser();
    }
    return null;
  }

  private IndexSearcher getSearcher() {
    try {
      return searcherManager.acquire();
    } catch (IOException e) {
      return null;
    }
  }

  private void release(IndexSearcher searcher) {
    try {
      if (searcher != null) {
        this.searcherManager.release(searcher);
      }
    } catch (IOException e) {
      //ignore
    }
  }

  @Override
  public long total() {
    final IndexSearcher searcher = getSearcher();
    try {
      return searcher == null ? -1 : searcher.getIndexReader().numDocs();
    } finally {
      release(searcher);
    }
  }

  @Override
  public Map<String, List<AggregateTerm>> aggregate(int topN, String... aggFieldName) {
    if (aggFieldName == null || aggFieldName.length == 0 || topN <= 0) {
      return Collections.emptyMap();
    }
    Map<String, Aggregation> aggregations = aggregator.getAggregations();
    Map<String, List<AggregateTerm>> result = new HashMap<>();
    for (String field : aggFieldName) {
      Aggregation aggregation = aggregations.get(field);
      if (aggregation != null) {
        List<AggregateTerm> terms = aggregation.terms();
        result.put(field, terms.stream().sorted(Comparator.comparingInt(AggregateTerm::getCounter).reversed())
          .limit(topN).collect(Collectors.toList()));
      }
    }
    return result;
  }

  @Override
  public void refreshSearcher() {
    try {
      searcherManager.maybeRefreshBlocking();
      aggregator.flushCache();
    } catch (IOException e) {
      log.error("Refresh search failed.", e);
    }
  }

  @Override
  public List<Document> search(Query query, SortField sort, int size) {
    if (query == null) {
      return Collections.emptyList();
    }
    Sort sortBy = sort == null ? null : new Sort(sort);
    TopFieldCollector collector = TopFieldCollector.create(sortBy, Math.max(1, size), null, size);
    final IndexSearcher searcher = getSearcher();
    try {
      if (searcher != null) {
        searcher.search(query, collector);
        TopDocs topDocs = collector.topDocs(0, Math.max(1, size));
        return Optional.ofNullable(topDocs.scoreDocs)
          .map(sc -> Stream.of(sc).map(doc -> {
            try {
              return searcher.doc(doc.doc);
            } catch (IOException e) {
              return null;
            }
          }).filter(Objects::nonNull).collect(Collectors.toList()))
          .orElse(Collections.emptyList());
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      release(searcher);
    }

    return Collections.emptyList();
  }

  @Override
  public int deleteAlbums(String albumId) {
    return 0;
  }

  @Override
  public int getTotalDocs(String field, String value) {
    IndexSearcher searcher = getSearcher();
    try {
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      builder.add(new TermQuery(new Term(field, value)), SHOULD);
      //TODO add delete filter
      BooleanQuery query = builder.build();
      SortField timestamp = new SortField("timestamp", SortField.Type.LONG);
      TopFieldCollector collector = TopFieldCollector.create(new Sort(timestamp), 1, Integer.MAX_VALUE);
      searcher.search(query, collector);

      int totalHits = collector.getTotalHits();

      return totalHits;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      release(searcher);
    }
    return 0;
  }

  private Set<String> getSetField(Document doc, String field) {
    return Optional.ofNullable(doc.getFields(field))
      .map(fields -> Arrays.stream(fields).map(IndexableField::stringValue).collect(Collectors.toSet()))
      .orElse(Collections.emptySet());
  }

  private static FieldType createType(boolean store, boolean tokenized) {
    FieldType ft = new FieldType();
    ft.setStored(store);
    ft.setTokenized(tokenized);
    return ft;
  }

  private void addStringCollection(Collection<String> values, Document document, String indexField, boolean store) {
    if (values != null) {
      for (String val : values) {
        document.add(new StringField(indexField, val, store ? YES : NO));
      }
    }
  }

  private String getOrDefault(String val, String def) {
    return val == null ? def : val;
  }

  private Document toDocument(Photo metadata) {
    LocalDateTime localTime = metadata.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

    Document document = new Document();
    document.add(new StringField("id", metadata.getId(), YES));
    document.add(new LatLonPoint("geo", metadata.getLatitude(), metadata.getLongitude()));
    document.add(new StringField("date", DateTools.dateToString(metadata.getDate(), Resolution.SECOND), YES));
    document.add(new NumericDocValuesField("timestamp", metadata.getDate().getTime()));
    document.add(new IntPoint("year", localTime.getYear()));
    document.add(new IntPoint("month", localTime.getMonthValue()));
    document.add(new IntPoint("favorite", metadata.getFavorite()));
    document.add(new StringField("deleted", String.valueOf(metadata.getDeleted()), NO));

    if (metadata.getLocation() != null) {
      document.add(new StringField("loc_nation", getOrDefault(metadata.getLocation().getNation(), ""), YES));
      document.add(new StringField("loc_province", getOrDefault(metadata.getLocation().getNation(), ""), YES));
      document.add(new StringField("loc_city", getOrDefault(metadata.getLocation().getNation(), ""), YES));
      document.add(new StringField("loc_district", getOrDefault(metadata.getLocation().getNation(), ""), YES));
      document.add(new StringField("loc_street", getOrDefault(metadata.getLocation().getNation(), ""), NO));
    }

    if (metadata.getCamera() != null) {
      document.add(new StringField("cam_maker", getOrDefault(metadata.getCamera().getMaker(), ""), YES));
    }

    addStringCollection(metadata.getAlbums(), document, "albums", true);
    addStringCollection(metadata.getTags(), document, "tags", true);

    return document;
  }

  public void close() {
    this.scheduledExecutorService.shutdownNow();
    Stream.of(this.writer, this.reader)
      .filter(Objects::nonNull).forEach(LucenePhotoIndexStore::close);
  }

  private static void close(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException e) {
      //ignore
    }
  }

  public static void main(String[] args) {
  }

}
