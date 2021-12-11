package org.molgenis.emx2.beacon.common;

public enum Granularity {
  BOOLEAN,
  COUNT,
  AGGREGATED,
  RECORD;

  public String toString() {
    return name().toLowerCase();
  }
}
