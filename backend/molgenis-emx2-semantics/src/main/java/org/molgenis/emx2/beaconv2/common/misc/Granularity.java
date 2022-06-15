package org.molgenis.emx2.beaconv2.common.misc;

public enum Granularity {
  BOOLEAN,
  COUNT,
  AGGREGATED,
  RECORD;

  public String toString() {
    return name().toLowerCase();
  }
}
