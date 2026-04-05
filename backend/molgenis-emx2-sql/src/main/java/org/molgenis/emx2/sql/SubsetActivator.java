package org.molgenis.emx2.sql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.BundleContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.SubsetEntry;
import org.molgenis.emx2.TableMetadata;

class SubsetActivator {

  private SubsetActivator() {}

  static Set<String> resolveTransitiveClosure(String subsetName, BundleContext bundleContext) {
    Set<String> resolved = new HashSet<>();
    collectTransitive(
        subsetName,
        bundleContext.getSubsetRegistry(),
        bundleContext.getTemplateRegistry(),
        resolved);
    return resolved;
  }

  private static void collectTransitive(
      String entryName,
      Map<String, SubsetEntry> subsetRegistry,
      Map<String, SubsetEntry> templateRegistry,
      Set<String> visited) {
    if (visited.contains(entryName)) return;
    visited.add(entryName);
    SubsetEntry entry =
        subsetRegistry.containsKey(entryName)
            ? subsetRegistry.get(entryName)
            : templateRegistry.get(entryName);
    if (entry != null) {
      for (String included : entry.getIncludes()) {
        collectTransitive(included, subsetRegistry, templateRegistry, visited);
      }
    }
  }

  static SchemaMetadata projectSchemaMetadataToActiveSubsets(
      SchemaMetadata fullSchema, Set<String> activeSubsets) {
    SchemaMetadata projected = new SchemaMetadata();

    for (TableMetadata bundleTable : fullSchema.getTables()) {
      if (!isEntryActive(bundleTable.getSubsets(), activeSubsets)) continue;

      TableMetadata projectedTable =
          new TableMetadata(bundleTable.getTableName())
              .setTableType(bundleTable.getTableType())
              .setInheritNames(bundleTable.getInheritNames())
              .setSubsets(bundleTable.getSubsets())
              .setSemantics(bundleTable.getSemantics());

      for (Column bundleColumn : bundleTable.getNonInheritedColumns()) {
        if (bundleColumn.isHeading()) continue;
        if (!isEntryActive(bundleColumn.getSubsets(), activeSubsets)) continue;
        projectedTable.add(bundleColumn);
      }

      projected.create(projectedTable);
    }

    return projected;
  }

  private static boolean isEntryActive(String[] entrySubsets, Set<String> activeSet) {
    if (entrySubsets == null || entrySubsets.length == 0) return true;
    for (String subset : entrySubsets) {
      if (activeSet.contains(subset)) return true;
    }
    return false;
  }
}
