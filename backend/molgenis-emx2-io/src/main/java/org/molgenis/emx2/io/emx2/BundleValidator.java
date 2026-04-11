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
      String bundleName, Map<String, ProfileEntry> profileRegistry, SchemaMetadata schema) {

    List<String> errors = new ArrayList<>();

    validateIdentifierFormats(profileRegistry, bundleName, errors);
    validateIncludesResolution(profileRegistry, bundleName, errors);
    validateIncludesAcyclicity(profileRegistry, errors);
    validateProfileReferencesOnTablesAndColumns(schema, profileRegistry, errors);
    validateReferenceCompleteness(schema, profileRegistry, errors);

    if (!errors.isEmpty()) {
      throw new MolgenisException(
          "Bundle '" + bundleName + "' has validation errors:\n" + String.join("\n", errors));
    }
  }

  private static final String IDENTIFIER_PATTERN = "[a-z][a-z0-9_]*";

  private static void validateIdentifierFormats(
      Map<String, ProfileEntry> profileRegistry, String bundleName, List<String> errors) {
    for (String id : profileRegistry.keySet()) {
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

  private static void validateProfileReferencesOnTablesAndColumns(
      SchemaMetadata schema, Map<String, ProfileEntry> combined, List<String> errors) {
    for (TableMetadata table : schema.getTables()) {
      if (table.getProfiles() != null) {
        for (String profileName : table.getProfiles()) {
          if (!combined.containsKey(profileName)) {
            errors.add(
                "Table '"
                    + table.getTableName()
                    + "' references unknown profile '"
                    + profileName
                    + "'");
          }
        }
      }
      for (Column column : table.getNonInheritedColumns()) {
        if (column.getProfiles() != null) {
          for (String profileName : column.getProfiles()) {
            if (!combined.containsKey(profileName)) {
              errors.add(
                  "Table '"
                      + table.getTableName()
                      + "', column '"
                      + column.getName()
                      + "' references unknown profile '"
                      + profileName
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

    Map<String, Set<String>> tableProfiles = buildTableProfilesMap(schema);
    Map<String, Set<String>> transitiveClosurePerEntry = buildTransitiveClosurePerEntry(combined);

    for (TableMetadata table : schema.getTables()) {
      Set<String> sourceProfiles = tableProfiles.getOrDefault(table.getTableName(), Set.of());
      if (sourceProfiles.isEmpty()) {
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
        Set<String> targetProfiles = tableProfiles.get(refTableName);
        if (targetProfiles == null || targetProfiles.isEmpty()) {
          continue;
        }

        boolean covered =
            existsActivationCoveringBoth(
                sourceProfiles, targetProfiles, combined, transitiveClosurePerEntry);
        if (!covered) {
          errors.add(
              "Table '"
                  + table.getTableName()
                  + "', column '"
                  + column.getName()
                  + "' (profiles: "
                  + sortedJoin(sourceProfiles)
                  + ") references table '"
                  + refTableName
                  + "' (profiles: "
                  + sortedJoin(targetProfiles)
                  + ") but no single profile in the registry covers both when activated. "
                  + "Either make '"
                  + refTableName
                  + "' always-on (no profiles tag) or add a profile whose transitive includes cover both.");
        }
      }
    }
  }

  private static boolean existsActivationCoveringBoth(
      Set<String> sourceProfiles,
      Set<String> targetProfiles,
      Map<String, ProfileEntry> combined,
      Map<String, Set<String>> transitiveClosurePerEntry) {

    for (String entryId : combined.keySet()) {
      Set<String> coverage = new HashSet<>();
      coverage.add(entryId);
      coverage.addAll(transitiveClosurePerEntry.getOrDefault(entryId, Set.of()));

      boolean coversSource = sourceProfiles.stream().anyMatch(coverage::contains);
      boolean coversTarget = targetProfiles.stream().anyMatch(coverage::contains);
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

  private static Map<String, Set<String>> buildTableProfilesMap(SchemaMetadata schema) {
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
