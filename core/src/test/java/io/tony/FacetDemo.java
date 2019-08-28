package io.tony;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

/**
 * Demonstrates indexing categories into different indexed fields.
 */
public class FacetDemo {

  private static final Path p = Paths.get("D:\\MyDocuments\\Source\\photo-manager\\target");
  private final Directory indexDir;
  private final Directory taxoDir;
  private final FacetsConfig config = new FacetsConfig();

  /**
   * Creates a new instance and populates the category list params mapping.
   */
  public FacetDemo() throws IOException {

    indexDir = FSDirectory.open(p.resolve("index"));
    taxoDir = FSDirectory.open(p.resolve("taxo"));
    config.setIndexFieldName("Author", "author");
    config.setIndexFieldName("Publish Date", "pubdate");
    config.setIndexFieldName("Tags", "tags");
    config.setHierarchical("Publish Date", true);
    config.setMultiValued("Tags", true);
  }

  /**
   * Build the example index.
   */
  private void index() throws IOException {
    IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig().setOpenMode(OpenMode.CREATE));

    // Writes facet ords to a separate directory from the main index
    DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);

    Document doc = new Document();
    doc.add(new StringField("Author", "Bob", Field.Store.YES));
    doc.add(new StringField("tags", "Arts & Photography", Field.Store.YES));
    doc.add(new StringField("tags", "Business & Money", Field.Store.YES));
    doc.add(new FacetField("Author", "Bob"));
    doc.add(new FacetField("Publish Date", "2010", "10", "15"));
    doc.add(new FacetField("Tags", "Arts & Photography"));
    doc.add(new FacetField("Tags", "Business & Money"));
    doc.add(new LatLonPoint("geo", 23.661, 24.772));


    indexWriter.addDocument(config.build(taxoWriter, doc));

    doc = new Document();
    doc.add(new StringField("Author", "Lisa", Field.Store.YES));
    doc.add(new FacetField("Author", "Lisa"));
    doc.add(new FacetField("Tags", "History"));
    doc.add(new FacetField("Publish Date", "2010", "10", "20"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    doc = new Document();
    doc.add(new StringField("Author", "Lisa", Field.Store.YES));
    doc.add(new FacetField("Author", "Lisa"));
    doc.add(new FacetField("Tags", "Medical Books"));
    doc.add(new FacetField("Publish Date", "2012", "1", "1"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    doc = new Document();
    doc.add(new StringField("Author", "Susan", Field.Store.YES));
    doc.add(new FacetField("Author", "Susan"));
    doc.add(new FacetField("Tags", "Children's Books"));
    doc.add(new FacetField("Publish Date", "2012", "1", "7"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    doc = new Document();
    doc.add(new StringField("Author", "Frank", Field.Store.YES));
    doc.add(new FacetField("Author", "Frank"));
//    doc.add(new FacetField("Tags", "Law"));
    doc.add(new FacetField("Publish Date", "1999", "5", "5"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    indexWriter.close();
    taxoWriter.close();
  }

  /**
   * User runs a query and counts facets.
   */
  private List<FacetResult> search() throws IOException {
    DirectoryReader indexReader = DirectoryReader.open(indexDir);
    IndexSearcher searcher = new IndexSearcher(indexReader);
    TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

    FacetsCollector fc = new FacetsCollector();

    // MatchAllDocsQuery is for "browsing" (counts facets
    // for all non-deleted docs in the index); normally
    // you'd use a "normal" query:
//    MatchAllDocsQuery q = new MatchAllDocsQuery();
    TermQuery q = new TermQuery(new Term("Author", "Lisa"));


    TopDocs documents = FacetsCollector.search(searcher, q, 10, fc);
    Arrays.stream(documents.scoreDocs)
      .forEach(doc -> {
        try {
          Document document = searcher.doc(doc.doc);
          System.out.println("=============================");
          document.getFields().forEach(f -> {
            String name = f.name();
            String s = f.stringValue();
            System.out.println(name + ":" + s);
          });
          System.out.println("=============================\n\n");
        } catch (IOException e) {
          e.printStackTrace();
        }

      });


    // Retrieve results
    List<FacetResult> results = new ArrayList<>();

    // Count both "Publish Date" and "Author" dimensions
    Facets author = new FastTaxonomyFacetCounts("author", taxoReader, config, fc);
    results.add(author.getTopChildren(10, "Author"));

    Facets pubDate = new FastTaxonomyFacetCounts("pubdate", taxoReader, config, fc);

    results.add(pubDate.getTopChildren(10, "Publish Date"));

    Facets tags = new FastTaxonomyFacetCounts("tags", taxoReader, config, fc);
    results.add(tags.getTopChildren(10, "Tags"));

    indexReader.close();
    taxoReader.close();

    return results;
  }

  /**
   * Runs the search example.
   */
  public List<FacetResult> runSearch() throws IOException {
    index();
    return search();
  }

  /**
   * Runs the search example and prints the results.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Facet counting over multiple category lists example:");
    System.out.println("-----------------------");
    List<FacetResult> results = new FacetDemo().runSearch();
    System.out.println("Author: " + results.get(0));
    System.out.println("Publish Date: " + results.get(1));
    System.out.println("Tags: " + results.get(2));
  }
}
