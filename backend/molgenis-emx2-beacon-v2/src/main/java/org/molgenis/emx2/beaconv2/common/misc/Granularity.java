package org.molgenis.emx2.beaconv2.common.misc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Granularity {
  BOOLEAN("boolean"),
  COUNT("count"),
  AGGREGATED("aggregated"),
  RECORD("record"),
  UNDEFINED("undefined");

  private String key;

  Granularity(String key) {
    this.key = key;
  }

  @JsonCreator
  public static Granularity fromString(String key) {
    return key == null ? null : Granularity.valueOf(key.toUpperCase());
  }

  @JsonValue
  public String getKey() {
    return key;
  }
}
