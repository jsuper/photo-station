package io.tony.photo.service.impl.agg;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.function.Function;

import io.tony.photo.utils.Strings;

public class StringAggregation extends BaseAggregation<String> {
  public StringAggregation() {
  }

  public StringAggregation(Function<String, String> termMapper) {
    super(termMapper);
  }

  @Override
  public String readValue(IndexableField field) {
    return field.stringValue();
  }

  @Override
  public boolean allowAggregate(String val) {
    return !Strings.isBlank(val);
  }
}
