package org.molgenis.emx2.io.emx2;

import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.ProfileEntry;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class BundleValidator {

  private BundleValidator() {}

  static void validate(
      String bundleName, Map<String, ProfileEntry> templateRegistry, SchemaMetadata schema) {

    List<String> errors = new ArrayList<>();

    validateIdentifierFormats(templateRegistry, bundleName, errors);
    validateIncludesResolution(templateRegistry, bundleName, errors);
    validateIncludesAcyclicity(templateRegistry, errors);
    validateSubsetReferencesOnTablesAndColumns(schema, templateRegistry, errors);
    validateReferenceCompleteness(schema, templateRegistry, errors);

    if (!errors.isEmpty()) {
      throw new MolgenisException(
          "Bundle '" + bundleName + "' has validation errors:\n" + String.join("\n", errors));
    }
  }

  private static final String IDENTIFIER_PATTERN = "[a-z][a-z0-9_]*";

  private static void validateIdentifierFormats(
      Map<String, ProfileEntry> templateRegistry, String bundleName, List<String> errors) {
    for (String id : templateRegistry.keySet()) {
      if (!id.matches(IDENTIFIER_PATTERN)) {
        errors.add(
            "Bundle '"
                + bundleName
                + "': invalid profile identifier '"
                + id
                + "'; must match [a-z][a-z0-9_]*");
      }
    }
  }

  private static void validateIncludesResolution(
      Map<String, ProfileEntry> combined, String bundleName, List<String> errors) {
    for (ProfileEntry entry : combined.values()) {
      for (String includedName : entry.getIncludes()) {
        if (!combined.containsKey(includedName)) {
          errors.add(
              "'"
                  + entry.getId()
                  + "' includes '"
                  + includedName
                  + "' which is not defined in the bundle '"
                  + bundleName
                  + "'");
        }
      }
    }
  }

  private static void validateIncludesAcyclicity(
      Map<String, ProfileEntry> combined, List<String> errors) {
    Set<String> permanentlyVisited = new HashSet<>();
    Set<String> inProgress = new LinkedHashSet<>();

    for (String startNode : combined.keySet()) {
      if (!permanentlyVisited.contains(startNode)) {
        detectCycle(startNode, combined, permanentlyVisited, inProgress, errors);
      }
    }
  }

  private static void detectCycle(
      String node,
      Map<String, ProfileEntry> combined,
      Set<String> permanentlyVisited,
      Set<String> inProgress,
      List<String> errors) {

    inProgress.add(node);

    ProfileEntry entry = combined.get(node);
    if (entry != null) {
      for (String neighbor : entry.getIncludes()) {
        if (!combined.containsKey(neighbor)) {
          continue;
        }
        if (inProgress.contains(neighbor)) {
          List<String> cycle = buildCyclePath(inProgress, neighbor);
          errors.add(
              "Circular includes detected: " + String.join(" -> ", cycle) + " -> " + neighbor);
          return;
        }
        if (!permanentlyVisited.contains(neighbor)) {
          detectCycle(neighbor, combined, permanentlyVisited, inProgress, errors);
        }
      }
    }

    inProgress.remove(node);
    permanentlyVisited.add(node);
  }

  private static List<String> buildCyclePath(Set<String> inProgress, String cycleStart) {
    List<String> path = new ArrayList<>(inProgress);
    int startIdx = path.indexOf(cycleStart);
    if (startIdx >= 0) {
      return path.subList(startIdx, path.size());
    }
    return path;
  }

  private static void validateSubsetReferencesOnTablesAndColumns(
      SchemaMetadata schema, Map<String, ProfileEntry> combined, List<String> errors) {
    for (TableMetadata table : schema.getTables()) {
      if (table.getProfiles() != null) {
        for (String subsetName : table.getProfiles()) {
          if (!combined.containsKey(subsetName)) {
            errors.add(
                "Table '"
                    + table.getTableName()
                    + "' references unknown subset '"
                    + subsetName
                    + "'");
          }
        }
      }
      for (Column column : table.getNonInheritedColumns()) {
        if (column.getProfiles() != null) {
          for (String subsetName : column.getProfiles()) {
            if (!combined.containsKey(subsetName)) {
              errors.add(
                  "Table '"
                      + table.getTableName()
                      + "', column '"
                      + column.getName()
                      + "' references unknown subset '"
                      + subsetName
                      + "'");
            }
          }
        }
      }
    }
  }

  private static void validateReferenceCompleteness(
      SchemaMetadata schema, Map<String, ProfileEntry> combined, List<String> errors) {

    if (combined.isEmpty()) {
      return;
    }

    Map<String, Set<String>> tableSubsets = buildTableSubsetsMap(schema);
    Map<String, Set<String>> transitiveClosurePerEntry = buildTransitiveClosurePerEntry(combined);

    for (TableMetadata table : schema.getTables()) {
      Set<String> sourceSubsets = tableSubsets.getOrDefault(table.getTableName(), Set.of());
      if (sourceSubsets.isEmpty()) {
        continue;
      }

      for (Column column : table.getNonInheritedColumns()) {
        if (!isRefColumn(column)) {
          continue;
        }
        String refTableName = column.getRefTableName();
        if (refTableName == null) {
          continue;
        }
        Set<String> targetSubsets = tableSubsets.get(refTableName);
        if (targetSubsets == null || targetSubsets.isEmpty()) {
          continue;
        }

        boolean covered =
            existsActivationCoveringBoth(
                sourceSubsets, targetSubsets, combined, transitiveClosurePerEntry);
        if (!covered) {
          errors.add(
              "Table '"
                  + table.getTableName()
                  + "', column '"
                  + column.getName()
                  + "' (subsets: "
                  + sortedJoin(sourceSubsets)
                  + ") references table '"
                  + refTableName
                  + "' (subsets: "
                  + sortedJoin(targetSubsets)
                  + ") but no single profile in the registry covers both when activated. "
                  + "Either make '"
                  + refTableName
                  + "' always-on (no profiles tag) or add a profile whose transitive includes cover both.");
        }
      }
    }
  }

  private static boolean existsActivationCoveringBoth(
      Set<String> sourceSubsets,
      Set<String> targetSubsets,
      Map<String, ProfileEntry> combined,
      Map<String, Set<String>> transitiveClosurePerEntry) {

    for (String entryId : combined.keySet()) {
      Set<String> coverage = new HashSet<>();
      coverage.add(entryId);
      coverage.addAll(transitiveClosurePerEntry.getOrDefault(entryId, Set.of()));

      boolean coversSource = sourceSubsets.stream().anyMatch(coverage::contains);
      boolean coversTarget = targetSubsets.stream().anyMatch(coverage::contains);
      if (coversSource && coversTarget) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, Set<String>> buildTransitiveClosurePerEntry(
      Map<String, ProfileEntry> combined) {
    Map<String, Set<String>> result = new HashMap<>();
    for (String id : combined.keySet()) {
      result.put(id, computeTransitiveIncludes(id, combined, new HashSet<>()));
    }
    return result;
  }

  private static boolean isRefColumn(Column column) {
    ColumnType type = column.getColumnType();
    return type != null && type.isReference();
  }

  private static Map<String, Set<String>> buildTableSubsetsMap(SchemaMetadata schema) {
    Map<String, Set<String>> result = new HashMap<>();
    for (TableMetadata table : schema.getTables()) {
      if (table.getProfiles() != null && table.getProfiles().length > 0) {
        result.put(table.getTableName(), new HashSet<>(Arrays.asList(table.getProfiles())));
      }
    }
    return result;
  }

  private static Set<String> computeTransitiveIncludes(
      String id, Map<String, ProfileEntry> combined, Set<String> visiting) {
    if (visiting.contains(id)) {
      return Set.of();
    }
    visiting.add(id);
    Set<String> result = new HashSet<>();
    ProfileEntry entry = combined.get(id);
    if (entry != null) {
      for (String included : entry.getIncludes()) {
        result.add(included);
        result.addAll(computeTransitiveIncludes(included, combined, visiting));
      }
    }
    visiting.remove(id);
    return result;
  }

  private static String sortedJoin(Set<String> items) {
    return String.join(", ", items.stream().sorted().toList());
  }
}
