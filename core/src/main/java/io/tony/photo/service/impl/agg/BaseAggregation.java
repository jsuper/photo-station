package io.tony.photo.service.impl.agg;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseAggregation<T> implements Aggregation<T> {

  private Map<String, AggregateTerm> terms = new HashMap<>();

  private Function<T, String> termMapper = t -> String.valueOf(t);

  public BaseAggregation() {
  }

  public BaseAggregation(Function<T, String> termMapper) {
    this.termMapper = termMapper;
  }

  @Override
  public void aggregate(IndexableField values) {
    T val = readValue(values);
    if (allowAggregate(val)) {
      addTerm(val);
    }
  }

  public void addTerm(T term) {
    String mappedTerm = termMapper.apply(term);
    terms.compute(mappedTerm, (t, o) -> o == null ? new AggregateTerm(mappedTerm) : o.incr());
  }

  @Override
  public int total(String val) {
    AggregateTerm aggregateTerm = terms.get(val);
    if (aggregateTerm != null) {
      return aggregateTerm.getCounter();
    }
    return 0;
  }

  public List<AggregateTerm> terms() {
    return new ArrayList<>(terms.values());
  }
}
