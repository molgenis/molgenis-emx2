package org.molgenis.emx2.io.emx2;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explicit deletions carried by a bundle. In the additive apply model absence never deletes;
 * removal happens only through {@code drop: true} markers, collected here as the set of tables and
 * the per-table columns to drop.
 */
public record ModelDrops(Set<String> tables, Map<String, List<String>> columns) {

  public static ModelDrops empty() {
    return new ModelDrops(Set.of(), Map.of());
  }
}
