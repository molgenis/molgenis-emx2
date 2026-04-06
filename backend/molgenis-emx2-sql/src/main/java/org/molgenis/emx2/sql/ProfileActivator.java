package org.molgenis.emx2.sql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.BundleContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ProfileEntry;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class ProfileActivator {

  private ProfileActivator() {}

  static Set<String> resolveTransitiveClosure(String profileName, BundleContext bundleContext) {
    Set<String> resolved = new HashSet<>();
    collectTransitive(
        profileName,
        bundleContext.getProfileRegistry(),
        bundleContext.getTemplateRegistry(),
        resolved);
    return resolved;
  }

  private static void collectTransitive(
      String entryName,
      Map<String, ProfileEntry> profileRegistry,
      Map<String, ProfileEntry> templateRegistry,
      Set<String> visited) {
    if (visited.contains(entryName)) return;
    visited.add(entryName);
    ProfileEntry entry =
        profileRegistry.containsKey(entryName)
            ? profileRegistry.get(entryName)
            : templateRegistry.get(entryName);
    if (entry != null) {
      for (String included : entry.getIncludes()) {
        collectTransitive(included, profileRegistry, templateRegistry, visited);
      }
    }
  }

  static SchemaMetadata projectSchemaMetadataToActiveProfiles(
      SchemaMetadata fullSchema, Set<String> activeProfiles) {
    SchemaMetadata projected = new SchemaMetadata();

    for (TableMetadata bundleTable : fullSchema.getTables()) {
      if (!isEntryActive(bundleTable.getProfiles(), activeProfiles)) continue;

      TableMetadata projectedTable =
          new TableMetadata(bundleTable.getTableName())
              .setTableType(bundleTable.getTableType())
              .setInheritNames(bundleTable.getInheritNames())
              .setProfiles(bundleTable.getProfiles())
              .setSemantics(bundleTable.getSemantics());

      for (Column bundleColumn : bundleTable.getNonInheritedColumns()) {
        if (bundleColumn.isHeading()) continue;
        if (!isEntryActive(bundleColumn.getProfiles(), activeProfiles)) continue;
        projectedTable.add(bundleColumn);
      }

      projected.create(projectedTable);
    }

    return projected;
  }

  private static boolean isEntryActive(String[] entryProfiles, Set<String> activeSet) {
    if (entryProfiles == null || entryProfiles.length == 0) return true;
    for (String profile : entryProfiles) {
      if (activeSet.contains(profile)) return true;
    }
    return false;
  }
}
