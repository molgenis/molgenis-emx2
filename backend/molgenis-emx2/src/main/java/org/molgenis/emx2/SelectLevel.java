package org.molgenis.emx2;

/** Granularity of SELECT access for a table permission. Ordered from least to most access. */
public enum SelectLevel {
  EXISTS,
  RANGE,
  AGGREGATOR,
  COUNT,
  TABLE,
  ROW
}
