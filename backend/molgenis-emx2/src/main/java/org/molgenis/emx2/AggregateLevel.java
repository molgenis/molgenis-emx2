package org.molgenis.emx2;

/**
 * Represents the effective aggregate access tier for a user on a given table. Ordered from least to
 * most permissive — each level implies all lower levels.
 */
public enum AggregateLevel {
  NONE,
  EXISTS,
  RANGE,
  AGGREGATOR,
  COUNT,
  FULL
}
