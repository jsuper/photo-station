package io.tony.photo.service.impl.agg;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.List;

public interface Aggregation<T> {

  T readValue(IndexableField values);

  void aggregate(IndexableField field);

  boolean allowAggregate(T val);

  List<AggregateTerm> terms();
}
