package io.tony.photo.service.impl.agg;

import lombok.Getter;

@Getter
public class AggregateTerm {

  private String value;
  private volatile int counter;

  public AggregateTerm(String value) {
    this.value = value;
    this.counter = 1;
  }

  public AggregateTerm incr() {
    this.counter++;
    return this;
  }
}
