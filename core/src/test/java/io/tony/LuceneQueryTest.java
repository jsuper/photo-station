package io.tony;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LuceneQueryTest {
  Path indexes;
  FSDirectory directory;
  IndexReader reader;
  IndexSearcher searcher;

  @Before
  public void setUp() throws Exception {
    this.indexes = Paths.get("D:\\test-photos\\.index\\indexes");
    this.directory = FSDirectory.open(indexes);
    this.reader = DirectoryReader.open(directory);
    this.searcher = new IndexSearcher(this.reader);
  }

  @Test
  public void testMatchAll() throws Exception {
    Query q = new MatchAllDocsQuery();
    TopDocs search = this.searcher.search(q, 10);
    printDoc(search);
  }

  @Test
  public void testFieldNotExistsQuery() throws Exception {
    QueryParser parser = new QueryParser("deleted", new StandardAnalyzer());
    parser.setAllowLeadingWildcard(true);
    Query parse = parser.parse("deleted:0");

    System.out.println(parse);

    TopDocs search = this.searcher.search(parse, 10);
    printDoc(search);

  }

  @Test
  public void testSurroundQueryParse() throws Exception {
    SrndQuery parse = org.apache.lucene.queryparser.surround.parser.QueryParser.parse("(deleted:1) or (year:2019)");
    System.out.println(parse);


  }

  void printDoc(TopDocs search) throws Exception {
    System.out.println("Total Hints: " + search.totalHits);
    for (ScoreDoc scoreDoc : search.scoreDocs) {
      Document document = reader.document(scoreDoc.doc);
      String id = document.getField("id").stringValue();
      IndexableField deleted = document.getField("deleted");
      System.out.println("\tdoc: " + id + ", deleted: " + (deleted == null ? "null" : deleted.stringValue()));
    }
  }

  @After
  public void tearDown() throws Exception {
    this.reader.close();
    this.directory.close();
  }
}
