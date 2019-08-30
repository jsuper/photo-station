package io.tony.photo.service.impl;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.MultiCollectorManager;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.tony.photo.pojo.LocationInfo;
import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.utils.Bytes;
import io.tony.photo.utils.Json;

import static org.apache.lucene.document.Field.Store.YES;

public class LucenePhotoIndexStore implements PhotoIndexStore {

  public static final String TAG_FACET_NAME = "Tags";
  public static final String SHOOTING_DATES_FACET_NAME = "Shooting dates";
  public static final String ALBUMS_FACET_NAME = "Albums";

  private Path indexDataFolder;
  private IndexWriterConfig.OpenMode openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND;

  private IndexWriter writer;
  private IndexReader reader;
  private SearcherManager searcherManager;

  private DirectoryTaxonomyWriter facetWriter;
  private DirectoryTaxonomyReader facetReader;
  private FacetsConfig facetsConfig = new FacetsConfig();

  private AtomicReference<IndexSearcher> searcherHolder = new AtomicReference<>();
  private Map<String, String> facetFieldMap;

  public LucenePhotoIndexStore(Path indexFolder) {
    this.indexDataFolder = indexFolder;

    Path indexes = this.indexDataFolder.resolve("indexes");
    Path facets = this.indexDataFolder.resolve("facets");

    try {
      IndexWriterConfig iwc = new IndexWriterConfig();
      iwc.setOpenMode(openMode);

      FSDirectory storeFolder = FSDirectory.open(indexes);
      this.writer = new IndexWriter(storeFolder, iwc);
      this.reader = DirectoryReader.open(writer);
      this.searcherManager = new SearcherManager(writer, new SearcherFactory());
      this.searcherHolder.compareAndSet(null, searcherManager.acquire());

      FSDirectory facetDir = FSDirectory.open(facets);
      this.facetWriter = new DirectoryTaxonomyWriter(facetDir);
      this.facetWriter.commit();
      this.facetReader = new DirectoryTaxonomyReader(facetDir);
    } catch (IOException e) {
      throw new IllegalStateException("Create internal searcher and reader failed.", e);
    }
    facetsConfig.setIndexFieldName(TAG_FACET_NAME, "tags");
    facetsConfig.setMultiValued(TAG_FACET_NAME, true);

    facetsConfig.setIndexFieldName(SHOOTING_DATES_FACET_NAME, "shoot_date");
    facetsConfig.setHierarchical(SHOOTING_DATES_FACET_NAME, true);

    facetsConfig.setIndexFieldName(ALBUMS_FACET_NAME, "albums");
    facetsConfig.setMultiValued(ALBUMS_FACET_NAME, true);

    Map<String, String> fc = new HashMap<>();
    fc.put("tags", TAG_FACET_NAME);
    fc.put("shoot_date", SHOOTING_DATES_FACET_NAME);
    fc.put("albums", ALBUMS_FACET_NAME);
    this.facetFieldMap = Collections.unmodifiableMap(fc);
  }

  @Override
  public void index(PhotoMetadata photoMetadata) {
    index(Arrays.asList(photoMetadata));
  }

  @Override
  public void index(List<PhotoMetadata> photoMetadata) {
    if (photoMetadata != null && !photoMetadata.isEmpty()) {
      try {
        photoMetadata.stream().forEach(meta -> {
          try {
            Document document = toDocument(meta);
            this.writer.updateDocument(new Term("id", meta.getId()), facetsConfig.build(facetWriter, document));
//            this.writer.updateDocument(new Term("id", meta.getId()), document);
          } catch (IOException e) {
            throw new IllegalStateException("Index document failed.", e);
          }
        });
        writer.commit();
        facetWriter.commit();

        this.searcherHolder.updateAndGet((s) -> {
          try {
            return searcherManager.acquire();
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
      } catch (Exception e) {
        throw new IllegalStateException("Index failed.", e);
      }
    }
  }

  @Override
  public void get(String photoId) {
    try {
      TopDocs docs = searcherManager.acquire().search(new TermQuery(new Term("id", photoId)), 1);
      if (docs.totalHits.value == 1) {
        System.out.println(docs);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public List<PhotoMetadata> list(int from, int size, Map<String, Object> query) {
    Query imageQuery = null;

    if (query == null || query.isEmpty()) {
      imageQuery = new MatchAllDocsQuery();
    }

    int numDocs = from + size;
    int totalDocs = this.reader.numDocs();
    if (from > totalDocs) {
      return Collections.emptyList();
    }

    int numHint = Math.max(1, Math.min(numDocs, totalDocs));
    Sort sort = new Sort(new SortField("timestamp", SortField.Type.LONG, true));

    TopFieldCollector collector = TopFieldCollector.create(sort, numHint, null, 10000);


    try {
      /*FacetsCollector fc = new FacetsCollector();
      FacetsCollector.search(acquire, imageQuery, size, MultiCollector.wrap(collector, fc));
      Facets tag = new FastTaxonomyFacetCounts("tags", this.facetReader, this.facetsConfig, fc);
      FacetResult topChildren = tag.getTopChildren(10, TAG_FACET_NAME);*/
      final IndexSearcher searcher = searcherHolder.get();
      searcher.search(imageQuery, collector);
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
            .map(this::toPhotoMetadata)
            .collect(Collectors.toList())).orElse(Collections.emptyList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  @Override
  public long total() {
    return searcherHolder.get().getIndexReader().numDocs();
  }

  @Override
  public Map<String, Map<String, Long>> aggregate(int topN, String... aggFieldName) {
    if (aggFieldName == null || aggFieldName.length == 0 || topN <= 0) {
      return Collections.emptyMap();
    }
    Query matchAll = new MatchAllDocsQuery();

    FacetsCollector fc = new FacetsCollector();
    try {
      FacetsCollector.search(searcherHolder.get(), matchAll, 1, fc);
      Map<String, Map<String, Long>> facetResults = new HashMap<>();
      for (String aggIndexField : aggFieldName) {
        if (facetFieldMap.containsKey(aggIndexField)) {
          Facets facets = new FastTaxonomyFacetCounts(aggIndexField, facetReader, facetsConfig, fc);
          FacetResult facetResult = facets.getTopChildren(topN, facetFieldMap.get(aggIndexField));
          Map<String, Long> fieldFacet = Optional.ofNullable(facetResult).flatMap(f -> Optional.ofNullable(f.labelValues))
            .map(lv -> Arrays.stream(lv).collect(Collectors.toMap(l -> l.label, l -> l.value.longValue())))
            .orElse(Collections.emptyMap());
          if (!fieldFacet.isEmpty()) {
            facetResults.put(aggIndexField, fieldFacet);
          }
        }
      }
      return facetResults;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Collections.emptyMap();
  }

  private PhotoMetadata toPhotoMetadata(Document document) {
    String id = document.get("id");
    String path = document.get("path");
    String date = document.get("shootingDate");
    String type = document.get("type");
    String device = document.get("device");

    Long size = Optional.ofNullable(document.getField("size"))
      .flatMap(f -> Optional.ofNullable(f.numericValue())).map(n -> n.longValue()).orElse(0l);
    Set<String> allTags = getSetField(document, "tags");
    Set<String> album = getSetField(document, "album");

    String nation = document.get("nation");
    String province = document.get("province");
    String city = document.get("city");
    String district = document.get("district");
    String street = document.get("street");

    PhotoMetadata metadata = new PhotoMetadata();
    metadata.setId(id);
    metadata.setPath(path);
    try {
      metadata.setShootingDate(DateTools.stringToDate(date));
    } catch (ParseException e) {
      //
    }
    metadata.setType(type);
    metadata.setDevice(device);
    metadata.setSize(size);
    metadata.setTags(allTags);
    metadata.setAlbum(album);

    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setNation(nation);
    locationInfo.setProvince(province);
    locationInfo.setCity(city);
    locationInfo.setDistrict(district);
    locationInfo.setStreet(street);
    metadata.setLocationInfo(locationInfo);
    return metadata;

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

  private Document toDocument(PhotoMetadata metadata) {
    Document document = new Document();

    document.add(new StringField("id", metadata.getId(), YES));
    document.add(new StoredField("path", metadata.getPath().toString()));
    document.add(new StoredField("type", metadata.getType()));
    document.add(new StoredField("device", metadata.getDevice()));

    document.add(new Field("size", new BytesRef(Bytes.longToBytes(metadata.getSize())), createType(true, false)));
    document.add(new Field("shootingDate", DateTools.dateToString(metadata.getShootingDate(), Resolution.SECOND), createType(true, false)));
    document.add(new NumericDocValuesField("timestamp", metadata.getShootingDate().getTime()));

    LocalDateTime localTime = metadata.getShootingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    document.add(new FacetField(SHOOTING_DATES_FACET_NAME, String.valueOf(localTime.getYear()),
      String.valueOf(localTime.getMonthValue())));

    if (metadata.getTags() != null) {
      for (String tag : metadata.getTags()) {
        document.add(new StringField("tags", tag, YES));
        document.add(new FacetField(TAG_FACET_NAME, tag));
      }
    }

    if (metadata.getAlbum() != null) {
      for (String album : metadata.getAlbum()) {
        document.add(new StringField("album", album, YES));
        document.add(new FacetField(ALBUMS_FACET_NAME, album));
      }
    }

    if (metadata.getLongitude() > 0 && metadata.getLatitude() > 0) {
      document.add(new LatLonPoint("geo", metadata.getLatitude(), metadata.getLongitude()));
    }
    if (metadata.getLocationInfo() != null) {
      document.add(new StringField("nation", metadata.getLocationInfo().getNation(), YES));
      document.add(new StringField("province", metadata.getLocationInfo().getProvince(), YES));
      document.add(new StringField("city", metadata.getLocationInfo().getCity(), YES));
      document.add(new StringField("district", metadata.getLocationInfo().getDistrict(), YES));
      document.add(new StringField("street", metadata.getLocationInfo().getStreet(), YES));
    }
    return document;
  }

  public void close() {
    Stream.of(this.writer, this.facetWriter, this.reader, this.facetReader)
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
    LucenePhotoIndexStore store = new LucenePhotoIndexStore(Paths.get("D:\\photos\\.index"));
    Map<String, Map<String, Long>> tags = store.aggregate(10, "tags");
    System.out.println(tags);
/*
    List<PhotoMetadata> list = store.list(0, 10000, Collections.emptyMap());
    list.forEach(d -> System.out.println(d.getId()));*/
  }

}
