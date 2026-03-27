package org.molgenis.emx2;

/**
 * Represents the effective aggregate access tier for a user on a given table. Ordered from least to
 * most permissive — each level implies all lower levels. COUNT is the highest level and implies
 * full access (view, count, aggregate).
 */
public enum AggregateLevel {
  NONE,
  EXISTS,
  RANGE,
  AGGREGATOR,
  COUNT
}
