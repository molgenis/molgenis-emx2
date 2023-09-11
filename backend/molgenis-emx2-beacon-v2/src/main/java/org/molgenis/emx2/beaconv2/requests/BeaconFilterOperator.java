package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum BeaconFilterOperator {
  EQ("="),
  LT("<"),
  GT(">"),
  NOT("!"),
  LTE("<="),
  GTE(">=");

  private String value;

  BeaconFilterOperator(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
