package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Granularity {
  NO_ACCESS("noAccess"),
  BOOLEAN("boolean"),
  RANGE("range"),
  COUNT("count"),
  LIST("list"),
  DETAILED_LIST("detailedList");

  private final String key;

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
