package org.molgenis.emx2.beaconv2.common.misc;

/** fixme: still returns uppercase in JSON, cannot use */
public enum Granularity {
  BOOLEAN,
  COUNT,
  AGGREGATED,
  RECORD;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
