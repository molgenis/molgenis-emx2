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
      Map<String, ProfileDef> profiles) {

    Map<String, TableDef> tables = new LinkedHashMap<>();
    for (TableMetadata table : schema.getTables()) {
      if (isRootTable(table)) {
        TableDef tableDef = convertRootTable(schema, table);
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
        null,
        null,
        null,
        null,
        null);
  }

  public static Bundle convertWithAutoRegistry(
      SchemaMetadata schema, String bundleName, String bundleDescription) {
    Set<String> allProfiles = collectAllProfileNames(schema);
    Map<String, ProfileDef> profiles = new LinkedHashMap<>();
    for (String profile : allProfiles) {
      profiles.put(profile, new ProfileDef(null, List.of(), Boolean.TRUE));
    }
    if (!allProfiles.isEmpty()) {
      profiles.put("all", new ProfileDef(null, List.copyOf(allProfiles), null));
    }

    return convert(schema, bundleName, bundleDescription, profiles);
  }

  private static boolean isRootTable(TableMetadata table) {
    if (TableType.INTERNAL.equals(table.getTableType())) {
      return false;
    }
    String[] inheritNames = table.getInheritNames();
    return inheritNames == null || inheritNames.length == 0;
  }

  private static TableDef convertRootTable(SchemaMetadata schema, TableMetadata table) {
    String description = table.getDescriptions().get("en");
    List<String> inherits =
        table.getInheritNames() != null ? Arrays.asList(table.getInheritNames()) : List.of();
    List<String> tableProfiles = normalizeProfiles(table.getProfiles());
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

    List<TableMetadata> variantTableList = findSubtypeTables(schema, table.getTableName());
    Map<String, VariantDef> variants = new LinkedHashMap<>();
    for (TableMetadata variantTable : variantTableList) {
      variants.put(
          variantTable.getTableName(), convertVariantDef(variantTable, table.getTableName()));
    }

    Map<String, DataColumn> columns = convertColumns(table.getNonInheritedColumns());

    List<String> profilesOrNull = tableProfiles.isEmpty() ? null : tableProfiles;
    Map<String, VariantDef> variantsOrNull = variants.isEmpty() ? null : variants;
    Map<String, DataColumn> columnsOrNull = columns.isEmpty() ? null : columns;

    return new TableDef(
        description,
        inherits.isEmpty() ? null : inherits,
        profilesOrNull,
        semantics,
        internal,
        label,
        oldName,
        importSchema,
        variantsOrNull,
        columnsOrNull,
        null);
  }

  private static VariantDef convertVariantDef(TableMetadata variantTable, String defaultParent) {
    String[] inheritNames = variantTable.getInheritNames();
    List<String> inherits;
    if (inheritNames != null
        && inheritNames.length > 0
        && !(inheritNames.length == 1 && defaultParent.equals(inheritNames[0]))) {
      inherits = Arrays.asList(inheritNames);
    } else {
      inherits = List.of();
    }
    Boolean internal = TableType.INTERNAL.equals(variantTable.getTableType()) ? Boolean.TRUE : null;
    String description = variantTable.getDescriptions().get("en");
    return new VariantDef(inherits.isEmpty() ? null : inherits, description, internal);
  }

  private static Map<String, DataColumn> convertColumns(List<Column> columns) {
    Map<String, DataColumn> result = new LinkedHashMap<>();
    for (Column col : columns) {
      if (col.isSystemColumn()) {
        continue;
      }
      result.put(col.getName(), convertColumn(col));
    }
    return result;
  }

  private static DataColumn convertColumn(Column col) {
    ColumnType colType = col.getColumnType();
    String type = null;
    if (colType != null && !ColumnType.STRING.equals(colType)) {
      type = columnTypeToString(colType);
    }

    int keyVal = col.getKey();
    Integer key = keyVal > 0 ? keyVal : null;

    String description = col.getDescriptions().get("en");

    List<String> semantics =
        col.getSemantics() != null && col.getSemantics().length > 0
            ? Arrays.asList(col.getSemantics())
            : null;

    List<String> colProfiles = normalizeProfiles(col.getProfiles());
    List<String> subsetsOrNull = colProfiles.isEmpty() ? null : colProfiles;

    String label =
        col.getLabel() != null && !col.getLabel().equals(col.getName()) ? col.getLabel() : null;

    Boolean readonlyVal = col.isReadonly() != null && col.isReadonly() ? Boolean.TRUE : null;

    return new DataColumn(
        type,
        key,
        col.getRequired(),
        col.getDefaultValue(),
        col.getValidation(),
        col.getVisible(),
        col.getComputed(),
        readonlyVal,
        col.getRefTableName(),
        col.getRefLink(),
        col.getRefBack(),
        col.getRefLabel(),
        null,
        null,
        description,
        semantics,
        null,
        subsetsOrNull,
        label,
        null,
        null);
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

  private static List<TableMetadata> findSubtypeTables(
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
        String[] inheritNames = table.getInheritNames();
        if (inheritNames != null) {
          for (String parent : inheritNames) {
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
