package org.molgenis.emx2.io.emx2.bundle;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.TemplateNameNormalizer;

public class SchemaMetadataToBundle {

  public static final String TYPE_SUBTYPE = "subtype";
  public static final String TYPE_SUBTYPE_ARRAY = "subtype_array";

  private SchemaMetadataToBundle() {}

  public static Bundle convert(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      Map<String, TemplateDef> templates) {

    Map<String, TableDef> tables = new LinkedHashMap<>();
    for (TableMetadata table : schema.getTables()) {
      if (isRootTable(table)) {
        TableDef tableDef = convertRootTable(schema, table);
        tables.put(table.getTableName(), tableDef);
      }
    }

    return new Bundle(bundleName, bundleDescription, Map.of(), templates, tables);
  }

  public static Bundle convertWithAutoRegistry(
      SchemaMetadata schema, String bundleName, String bundleDescription) {
    Set<String> allTemplates = collectAllTemplateNames(schema);
    Map<String, TemplateDef> templates = new LinkedHashMap<>();
    for (String template : allTemplates) {
      templates.put(template, new TemplateDef(null, List.of(), Boolean.TRUE));
    }
    if (!allTemplates.isEmpty()) {
      templates.put("all", new TemplateDef(null, List.copyOf(allTemplates), null));
    }

    return convert(schema, bundleName, bundleDescription, templates);
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
    List<String> tableTemplates = normalizeTemplates(table.getSubsets());
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

    List<TableMetadata> subtypeTableList = findSubtypeTables(schema, table.getTableName());
    Map<String, SubtypeDef> subtypes = new LinkedHashMap<>();
    for (TableMetadata subtypeTable : subtypeTableList) {
      subtypes.put(
          subtypeTable.getTableName(), convertSubtypeDef(subtypeTable, table.getTableName()));
    }

    Map<String, DataColumn> columns = convertColumns(table.getNonInheritedColumns());

    List<String> templatesOrNull = tableTemplates.isEmpty() ? null : tableTemplates;
    Map<String, SubtypeDef> subtypesOrNull = subtypes.isEmpty() ? null : subtypes;
    Map<String, DataColumn> columnsOrNull = columns.isEmpty() ? null : columns;

    return new TableDef(
        description,
        inherits.isEmpty() ? null : inherits,
        templatesOrNull,
        semantics,
        internal,
        label,
        oldName,
        importSchema,
        subtypesOrNull,
        columnsOrNull,
        null);
  }

  private static SubtypeDef convertSubtypeDef(TableMetadata subtypeTable, String defaultParent) {
    String[] inheritNames = subtypeTable.getInheritNames();
    List<String> inherits;
    if (inheritNames != null
        && inheritNames.length > 0
        && !(inheritNames.length == 1 && defaultParent.equals(inheritNames[0]))) {
      inherits = Arrays.asList(inheritNames);
    } else {
      inherits = List.of();
    }
    Boolean internal = TableType.INTERNAL.equals(subtypeTable.getTableType()) ? Boolean.TRUE : null;
    String description = subtypeTable.getDescriptions().get("en");
    return new SubtypeDef(inherits.isEmpty() ? null : inherits, description, internal);
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

    List<String> templates = normalizeTemplates(col.getSubsets());
    List<String> subsetsOrNull = templates.isEmpty() ? null : templates;

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
    if (ColumnType.EXTENSION.equals(colType)) {
      return TYPE_SUBTYPE;
    }
    if (ColumnType.EXTENSION_ARRAY.equals(colType)) {
      return TYPE_SUBTYPE_ARRAY;
    }
    return colType.toString().toLowerCase();
  }

  private static List<String> normalizeTemplates(String[] templateNames) {
    if (templateNames == null || templateNames.length == 0) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String template : templateNames) {
      result.add(TemplateNameNormalizer.normalize(template));
    }
    return result;
  }

  private static Set<String> collectAllTemplateNames(SchemaMetadata schema) {
    Set<String> allTemplates = new LinkedHashSet<>();
    for (TableMetadata table : schema.getTables()) {
      if (table.getSubsets() != null) {
        for (String template : table.getSubsets()) {
          allTemplates.add(TemplateNameNormalizer.normalize(template));
        }
      }
      for (Column column : table.getNonInheritedColumns()) {
        if (column.getSubsets() != null) {
          for (String template : column.getSubsets()) {
            allTemplates.add(TemplateNameNormalizer.normalize(template));
          }
        }
      }
    }
    return allTemplates;
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
