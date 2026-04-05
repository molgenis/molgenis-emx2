package org.molgenis.emx2.io.emx2;

import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.SubsetEntry;
import org.molgenis.emx2.TableMetadata;

class BundleValidator {

  private BundleValidator() {}

  static void validate(
      String bundleName,
      Map<String, SubsetEntry> subsetRegistry,
      Map<String, SubsetEntry> templateRegistry,
      SchemaMetadata schema) {

    List<String> errors = new ArrayList<>();

    validateIdentifierFormats(subsetRegistry, templateRegistry, bundleName, errors);
    validateNamespaceUniqueness(subsetRegistry, templateRegistry, errors);

    Map<String, SubsetEntry> combined = combinedRegistry(subsetRegistry, templateRegistry);

    validateIncludesResolution(combined, bundleName, errors);
    validateIncludesAcyclicity(combined, errors);
    validateSubsetReferencesOnTablesAndColumns(schema, combined, errors);
    validateReferenceCompleteness(schema, combined, errors);

    if (!errors.isEmpty()) {
      throw new MolgenisException(
          "Bundle '" + bundleName + "' has validation errors:\n" + String.join("\n", errors));
    }
  }

  private static Map<String, SubsetEntry> combinedRegistry(
      Map<String, SubsetEntry> subsetRegistry, Map<String, SubsetEntry> templateRegistry) {
    Map<String, SubsetEntry> combined = new LinkedHashMap<>(subsetRegistry);
    combined.putAll(templateRegistry);
    return combined;
  }

  private static final String IDENTIFIER_PATTERN = "[a-z][a-z0-9_]*";

  private static void validateIdentifierFormats(
      Map<String, SubsetEntry> subsetRegistry,
      Map<String, SubsetEntry> templateRegistry,
      String bundleName,
      List<String> errors) {
    for (String id : subsetRegistry.keySet()) {
      if (!id.matches(IDENTIFIER_PATTERN)) {
        errors.add(
            "Bundle '"
                + bundleName
                + "': invalid subset identifier '"
                + id
                + "'; must match [a-z][a-z0-9_]*");
      }
    }
    for (String id : templateRegistry.keySet()) {
      if (!id.matches(IDENTIFIER_PATTERN)) {
        errors.add(
            "Bundle '"
                + bundleName
                + "': invalid template identifier '"
                + id
                + "'; must match [a-z][a-z0-9_]*");
      }
    }
  }

  private static void validateNamespaceUniqueness(
      Map<String, SubsetEntry> subsetRegistry,
      Map<String, SubsetEntry> templateRegistry,
      List<String> errors) {
    for (String id : templateRegistry.keySet()) {
      if (subsetRegistry.containsKey(id)) {
        errors.add(
            "Identifier '"
                + id
                + "' appears in both 'subsets:' and 'templates:'; names must be unique across both sections");
      }
    }
  }

  private static void validateIncludesResolution(
      Map<String, SubsetEntry> combined, String bundleName, List<String> errors) {
    for (SubsetEntry entry : combined.values()) {
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
      Map<String, SubsetEntry> combined, List<String> errors) {
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
      Map<String, SubsetEntry> combined,
      Set<String> permanentlyVisited,
      Set<String> inProgress,
      List<String> errors) {

    inProgress.add(node);

    SubsetEntry entry = combined.get(node);
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
      SchemaMetadata schema, Map<String, SubsetEntry> combined, List<String> errors) {
    for (TableMetadata table : schema.getTables()) {
      if (table.getSubsets() != null) {
        for (String subsetName : table.getSubsets()) {
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
        if (column.getSubsets() != null) {
          for (String subsetName : column.getSubsets()) {
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
      SchemaMetadata schema, Map<String, SubsetEntry> combined, List<String> errors) {

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
                  + ") but no single template or subset in the registry covers both when activated. "
                  + "Either make '"
                  + refTableName
                  + "' always-on (no subsets tag) or add a template/subset whose transitive includes cover both.");
        }
      }
    }
  }

  private static boolean existsActivationCoveringBoth(
      Set<String> sourceSubsets,
      Set<String> targetSubsets,
      Map<String, SubsetEntry> combined,
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
      Map<String, SubsetEntry> combined) {
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
      if (table.getSubsets() != null && table.getSubsets().length > 0) {
        result.put(table.getTableName(), new HashSet<>(Arrays.asList(table.getSubsets())));
      }
    }
    return result;
  }

  private static Set<String> computeTransitiveIncludes(
      String id, Map<String, SubsetEntry> combined, Set<String> visiting) {
    if (visiting.contains(id)) {
      return Set.of();
    }
    visiting.add(id);
    Set<String> result = new HashSet<>();
    SubsetEntry entry = combined.get(id);
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
