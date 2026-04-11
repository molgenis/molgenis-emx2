package org.molgenis.emx2.io.emx2.bundle;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.ProfileNameNormalizer;

public class SchemaMetadataToBundle {

  public static final String TYPE_VARIANT = "variant";
  public static final String TYPE_VARIANT_ARRAY = "variant_array";

  private SchemaMetadataToBundle() {}

  public static Bundle convert(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      List<ProfileDef> profiles) {
    return convert(schema, bundleName, bundleDescription, profiles, Map.of());
  }

  public static Bundle convert(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      List<ProfileDef> profiles,
      Map<String, List<String>> profileIncludes) {

    Map<String, TableDef> tables = new LinkedHashMap<>();
    for (TableMetadata table : schema.getTables()) {
      if (isRootTable(table)) {
        TableDef tableDef = convertRootTable(schema, table, profileIncludes);
        tables.put(table.getTableName(), tableDef);
      }
    }

    return new Bundle(
        bundleName,
        bundleDescription,
        Map.of(),
        profiles,
        tables,
        null,
        null,
        null,
        Map.of(),
        Map.of());
  }

  public static Bundle convertWithAutoRegistry(
      SchemaMetadata schema, String bundleName, String bundleDescription) {
    return convertWithAutoRegistry(schema, bundleName, bundleDescription, List.of());
  }

  public static Bundle convertWithAutoRegistry(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      List<ProfileDef> profileDefs) {
    Map<String, List<String>> profileIncludes = buildIncludesMap(profileDefs);
    Set<String> allProfiles = collectAllProfileNames(schema);
    List<ProfileDef> profiles = new ArrayList<>(profileDefs);
    Set<String> existingNames = new LinkedHashSet<>();
    for (ProfileDef def : profileDefs) {
      existingNames.add(def.name());
    }
    for (String profile : allProfiles) {
      if (!existingNames.contains(profile)) {
        profiles.add(new ProfileDef(profile, null, List.of(), Boolean.TRUE, List.of()));
      }
    }
    Set<String> allProfileNames = collectAllProfileNames(schema);
    for (ProfileDef def : profileDefs) {
      allProfileNames.add(def.name());
    }
    if (!allProfileNames.isEmpty()) {
      profiles.removeIf(p -> "all".equals(p.name()));
      profiles.add(new ProfileDef("all", null, List.copyOf(allProfileNames), null, List.of()));
    }

    return convert(schema, bundleName, bundleDescription, profiles, profileIncludes);
  }

  static Map<String, List<String>> buildIncludesMap(List<ProfileDef> profileDefs) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (ProfileDef def : profileDefs) {
      result.put(def.name(), def.includes());
    }
    return result;
  }

  static Set<String> computeTransitiveClosure(String profile, Map<String, List<String>> includes) {
    Set<String> visited = new LinkedHashSet<>();
    Queue<String> queue = new LinkedList<>();
    List<String> direct = includes.get(profile);
    if (direct != null) {
      queue.addAll(direct);
    }
    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (visited.add(current)) {
        List<String> next = includes.get(current);
        if (next != null) {
          queue.addAll(next);
        }
      }
    }
    return visited;
  }

  static List<String> deduplicateWithIncludes(
      List<String> profiles, Map<String, List<String>> allIncludes) {
    if (profiles.size() <= 1 || allIncludes.isEmpty()) {
      return profiles;
    }
    Set<String> profileSet = new LinkedHashSet<>(profiles);
    List<String> result = new ArrayList<>();
    for (String candidate : profiles) {
      Set<String> transitivelyIncluded = computeTransitiveClosure(candidate, allIncludes);
      boolean redundant = false;
      for (String other : profileSet) {
        if (!other.equals(candidate) && transitivelyIncluded.contains(other)) {
          redundant = true;
          break;
        }
      }
      if (!redundant) {
        result.add(candidate);
      }
    }
    return result;
  }

  private static boolean isRootTable(TableMetadata table) {
    if (TableType.INTERNAL.equals(table.getTableType())) {
      return false;
    }
    String[] extendNames = table.getExtendNames();
    return extendNames == null || extendNames.length == 0;
  }

  private static TableDef convertRootTable(
      SchemaMetadata schema, TableMetadata table, Map<String, List<String>> profileIncludes) {
    String description = table.getDescriptions().get("en");
    List<String> extendList =
        table.getExtendNames() != null ? Arrays.asList(table.getExtendNames()) : List.of();
    List<String> tableProfiles =
        deduplicateWithIncludes(normalizeProfiles(table.getProfiles()), profileIncludes);
    List<String> semantics =
        table.getSemantics() != null && table.getSemantics().length > 0
            ? Arrays.asList(table.getSemantics())
            : null;
    Boolean internal = TableType.INTERNAL.equals(table.getTableType()) ? Boolean.TRUE : null;
    String label =
        table.getLabel() != null && !table.getLabel().equals(table.getTableName())
            ? table.getLabel()
            : null;
    String oldName = table.getOldName();
    String importSchema = table.getImportSchema();

    List<TableMetadata> variantTableList = findVariantTables(schema, table.getTableName());
    List<VariantDef> variants = new ArrayList<>();
    for (TableMetadata variantTable : variantTableList) {
      variants.add(convertVariantDef(variantTable, table.getTableName()));
    }

    List<Map<String, Object>> columns =
        convertColumnsToList(table.getNonInheritedColumns(), tableProfiles, profileIncludes);
    for (TableMetadata variantTable : variantTableList) {
      List<Map<String, Object>> variantColumns =
          convertColumnsToList(
              variantTable.getNonInheritedColumns(), tableProfiles, profileIncludes);
      if (!variantColumns.isEmpty()) {
        Map<String, Object> variantGroup = new LinkedHashMap<>();
        variantGroup.put("variant", variantTable.getTableName());
        variantGroup.put("columns", variantColumns);
        columns.add(variantGroup);
      }
    }

    List<String> profilesOrNull = tableProfiles.isEmpty() ? null : tableProfiles;
    List<VariantDef> variantsOrNull = variants.isEmpty() ? null : variants;
    List<Map<String, Object>> columnsOrNull = columns.isEmpty() ? null : columns;

    return new TableDef(
        description,
        extendList.isEmpty() ? null : extendList,
        profilesOrNull,
        semantics,
        internal,
        label,
        oldName,
        importSchema,
        variantsOrNull,
        columnsOrNull);
  }

  private static VariantDef convertVariantDef(TableMetadata variantTable, String defaultParent) {
    String[] rawExtendNames = variantTable.getExtendNames();
    List<String> extendList;
    if (rawExtendNames != null
        && rawExtendNames.length > 0
        && !(rawExtendNames.length == 1 && defaultParent.equals(rawExtendNames[0]))) {
      extendList = Arrays.asList(rawExtendNames);
    } else {
      extendList = List.of();
    }
    Boolean internal = TableType.INTERNAL.equals(variantTable.getTableType()) ? Boolean.TRUE : null;
    String description = variantTable.getDescriptions().get("en");
    List<String> variantProfiles = normalizeProfiles(variantTable.getProfiles());
    return new VariantDef(
        variantTable.getTableName(),
        extendList.isEmpty() ? null : extendList,
        description,
        internal,
        variantProfiles.isEmpty() ? null : variantProfiles);
  }

  private static List<Map<String, Object>> convertColumnsToList(
      List<Column> columns, List<String> parentProfiles) {
    return convertColumnsToList(columns, parentProfiles, Map.of());
  }

  private static List<Map<String, Object>> convertColumnsToList(
      List<Column> columns,
      List<String> parentProfiles,
      Map<String, List<String>> profileIncludes) {
    List<Map<String, Object>> result = new ArrayList<>();
    List<Map<String, Object>> currentSectionColumns = null;
    List<Map<String, Object>> currentHeadingColumns = null;
    Map<String, Object> currentSectionEntry = null;
    Map<String, Object> currentHeadingEntry = null;
    List<String> currentSectionProfiles = null;
    List<String> currentHeadingProfiles = null;

    for (Column col : columns) {
      if (col.isSystemColumn()) {
        continue;
      }
      ColumnType colType = col.getColumnType();
      if (ColumnType.SECTION.equals(colType)) {
        currentSectionEntry =
            buildContainerEntryWithDedup("section", col, parentProfiles, profileIncludes);
        currentSectionColumns = new ArrayList<>();
        currentSectionProfiles =
            deduplicateWithIncludes(normalizeProfiles(col.getProfiles()), profileIncludes);
        currentSectionEntry.put("columns", currentSectionColumns);
        currentHeadingColumns = null;
        currentHeadingEntry = null;
        currentHeadingProfiles = null;
        result.add(currentSectionEntry);
      } else if (ColumnType.HEADING.equals(colType)) {
        List<String> parentForHeading =
            currentSectionProfiles != null ? currentSectionProfiles : parentProfiles;
        currentHeadingEntry =
            buildContainerEntryWithDedup("heading", col, parentForHeading, profileIncludes);
        currentHeadingColumns = new ArrayList<>();
        currentHeadingProfiles =
            deduplicateWithIncludes(normalizeProfiles(col.getProfiles()), profileIncludes);
        currentHeadingEntry.put("columns", currentHeadingColumns);
        if (currentSectionColumns != null) {
          currentSectionColumns.add(currentHeadingEntry);
        } else {
          result.add(currentHeadingEntry);
        }
      } else {
        List<String> effectiveParent =
            currentHeadingProfiles != null
                ? currentHeadingProfiles
                : (currentSectionProfiles != null ? currentSectionProfiles : parentProfiles);
        Map<String, Object> colMap = convertColumnToMap(col, effectiveParent, profileIncludes);
        if (currentHeadingColumns != null) {
          currentHeadingColumns.add(colMap);
        } else if (currentSectionColumns != null) {
          currentSectionColumns.add(colMap);
        } else {
          result.add(colMap);
        }
      }
    }

    liftCommonChildProfilesToContainers(result, parentProfiles);

    return result;
  }

  @SuppressWarnings("unchecked")
  private static void liftCommonChildProfilesToContainers(
      List<Map<String, Object>> entries, List<String> parentProfiles) {
    for (Map<String, Object> entry : entries) {
      if (!entry.containsKey("section") && !entry.containsKey("heading")) {
        continue;
      }
      if (entry.get("profiles") != null) {
        continue;
      }
      List<Map<String, Object>> children = (List<Map<String, Object>>) entry.get("columns");
      if (children == null || children.isEmpty()) {
        continue;
      }
      List<String> commonProfiles = extractCommonProfiles(children);
      if (commonProfiles == null || commonProfiles.isEmpty()) {
        continue;
      }
      if (profileSetsEqual(commonProfiles, parentProfiles)) {
        continue;
      }
      entry.put("profiles", commonProfiles);
      for (Map<String, Object> child : children) {
        if (child.containsKey("section") || child.containsKey("heading")) {
          continue;
        }
        Object childProfiles = child.get("profiles");
        if (childProfiles instanceof List
            && profileSetsEqual((List<String>) childProfiles, commonProfiles)) {
          child.remove("profiles");
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> extractCommonProfiles(List<Map<String, Object>> children) {
    List<String> common = null;
    for (Map<String, Object> child : children) {
      if (child.containsKey("section") || child.containsKey("heading")) {
        return null;
      }
      Object profiles = child.get("profiles");
      if (profiles == null) {
        return null;
      }
      List<String> childProfiles = (List<String>) profiles;
      if (common == null) {
        common = childProfiles;
      } else if (!profileSetsEqual(childProfiles, common)) {
        return null;
      }
    }
    return common;
  }

  private static boolean profileSetsEqual(List<String> a, List<String> b) {
    if (a == null || a.isEmpty()) return b == null || b.isEmpty();
    if (b == null || b.isEmpty()) return false;
    return new HashSet<>(a).equals(new HashSet<>(b));
  }

  private static Map<String, Object> buildContainerEntryWithDedup(
      String containerKey, Column col, List<String> parentProfiles) {
    return buildContainerEntryWithDedup(containerKey, col, parentProfiles, Map.of());
  }

  private static Map<String, Object> buildContainerEntryWithDedup(
      String containerKey,
      Column col,
      List<String> parentProfiles,
      Map<String, List<String>> profileIncludes) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put(containerKey, col.getName());
    String description = col.getDescriptions().get("en");
    if (description != null) {
      entry.put("description", description);
    }
    List<String> colProfiles =
        deduplicateWithIncludes(normalizeProfiles(col.getProfiles()), profileIncludes);
    if (!colProfiles.isEmpty() && !profileSetsEqual(colProfiles, parentProfiles)) {
      entry.put("profiles", colProfiles);
    }
    return entry;
  }

  static Map<String, Object> convertColumnToMap(Column col) {
    return convertColumnToMap(col, List.of(), Map.of());
  }

  static Map<String, Object> convertColumnToMap(Column col, List<String> parentProfiles) {
    return convertColumnToMap(col, parentProfiles, Map.of());
  }

  static Map<String, Object> convertColumnToMap(
      Column col, List<String> parentProfiles, Map<String, List<String>> profileIncludes) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("name", col.getName());

    ColumnType colType = col.getColumnType();
    if (colType != null && !ColumnType.STRING.equals(colType)) {
      entry.put("type", columnTypeToString(colType));
    }

    int keyVal = col.getKey();
    if (keyVal > 0) {
      entry.put("key", keyVal);
    }

    if (col.getRequired() != null) {
      String required = col.getRequired();
      if ("true".equalsIgnoreCase(required)) {
        entry.put("required", Boolean.TRUE);
      } else if ("false".equalsIgnoreCase(required)) {
        entry.put("required", Boolean.FALSE);
      } else {
        entry.put("required", required);
      }
    }
    if (col.getDefaultValue() != null) {
      entry.put("defaultValue", col.getDefaultValue());
    }
    if (col.getRefTableName() != null) {
      entry.put("refTable", col.getRefTableName());
    }
    if (col.getRefLink() != null) {
      entry.put("refLink", col.getRefLink());
    }
    if (col.getRefBack() != null) {
      entry.put("refBack", col.getRefBack());
    }
    if (col.getRefLabel() != null) {
      entry.put("refLabel", col.getRefLabel());
    }
    String description = col.getDescriptions().get("en");
    if (description != null) {
      entry.put("description", description);
    }
    if (col.getSemantics() != null && col.getSemantics().length > 0) {
      entry.put("semantics", Arrays.asList(col.getSemantics()));
    }
    List<String> colProfiles =
        deduplicateWithIncludes(normalizeProfiles(col.getProfiles()), profileIncludes);
    if (!colProfiles.isEmpty() && !profileSetsEqual(colProfiles, parentProfiles)) {
      entry.put("profiles", colProfiles);
    }
    if (col.getValidation() != null) {
      entry.put("validation", col.getValidation());
    }
    if (col.getVisible() != null) {
      entry.put("visible", col.getVisible());
    }
    if (col.getComputed() != null) {
      entry.put("computed", col.getComputed());
    }
    if (Boolean.TRUE.equals(col.isReadonly())) {
      entry.put("readonly", true);
    }
    String label =
        col.getLabel() != null && !col.getLabel().equals(col.getName()) ? col.getLabel() : null;
    if (label != null) {
      entry.put("label", label);
    }

    return entry;
  }

  private static String columnTypeToString(ColumnType colType) {
    if (ColumnType.VARIANT.equals(colType)) {
      return TYPE_VARIANT;
    }
    if (ColumnType.VARIANT_ARRAY.equals(colType)) {
      return TYPE_VARIANT_ARRAY;
    }
    return colType.toString().toLowerCase();
  }

  private static List<String> normalizeProfiles(String[] profileNames) {
    if (profileNames == null || profileNames.length == 0) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String profile : profileNames) {
      result.add(ProfileNameNormalizer.normalize(profile));
    }
    return result;
  }

  private static Set<String> collectAllProfileNames(SchemaMetadata schema) {
    Set<String> allProfiles = new LinkedHashSet<>();
    for (TableMetadata table : schema.getTables()) {
      if (table.getProfiles() != null) {
        for (String profile : table.getProfiles()) {
          allProfiles.add(ProfileNameNormalizer.normalize(profile));
        }
      }
      for (Column column : table.getNonInheritedColumns()) {
        if (column.getProfiles() != null) {
          for (String profile : column.getProfiles()) {
            allProfiles.add(ProfileNameNormalizer.normalize(profile));
          }
        }
      }
    }
    return allProfiles;
  }

  private static List<TableMetadata> findVariantTables(
      SchemaMetadata schema, String rootTableName) {
    List<TableMetadata> result = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new LinkedList<>();
    queue.add(rootTableName);
    visited.add(rootTableName);

    while (!queue.isEmpty()) {
      String current = queue.poll();
      for (TableMetadata table : schema.getTables()) {
        if (visited.contains(table.getTableName())) {
          continue;
        }
        String[] extendNames = table.getExtendNames();
        if (extendNames != null) {
          for (String parent : extendNames) {
            if (current.equals(parent)) {
              result.add(table);
              visited.add(table.getTableName());
              queue.add(table.getTableName());
              break;
            }
          }
        }
      }
    }
    return result;
  }
}
