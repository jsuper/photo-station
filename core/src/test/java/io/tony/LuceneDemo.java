package io.tony;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LuceneDemo {

  public static void main(String[] args) throws Exception {
    Path indexes = Paths.get("D:\\photos\\.index\\indexes");
    FSDirectory directory = FSDirectory.open(indexes);

    IndexReader reader = DirectoryReader.open(directory);

    IndexSearcher searcher = new IndexSearcher(reader);
    Map<String, Integer> counter = new HashMap<>();
    searcher.search(new MatchAllDocsQuery(), new Collector() {
      @Override
      public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
        return new LeafCollector() {
          @Override
          public void setScorer(Scorable scorer) throws IOException {

          }

          @Override
          public void collect(int doc) throws IOException {
            int docId = context.docBase + doc;
            Document document = reader.document(docId);
            IndexableField[] tags = document.getFields("tags");
            if (tags != null && tags.length > 0) {
              for (IndexableField tag : tags) {
                String value = tag.stringValue();
                counter.compute(value, (key, old) -> old == null ? 1 : old.intValue() + 1);
              }
            }
          }
        };
      }

      @Override
      public ScoreMode scoreMode() {
        return ScoreMode.COMPLETE;
      }
    });

    System.out.println(counter);
  }
}
