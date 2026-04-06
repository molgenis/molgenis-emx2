package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.bundle.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Emx2Yaml {

  private static final String FIELD_TABLE = "table";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_ACTIVE_SUBSETS = "activeSubsets";
  private static final String FIELD_COLUMNS = "columns";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_INHERITS = "inherits";
  private static final String FIELD_INTERNAL = "internal";
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_KEY = "key";
  private static final String FIELD_REQUIRED = "required";
  private static final String FIELD_DEFAULT_VALUE = "defaultValue";
  private static final String FIELD_REF_TABLE = "refTable";
  private static final String FIELD_SEMANTICS = "semantics";
  private static final String FIELD_VALIDATION = "validation";
  private static final String FIELD_VISIBLE = "visible";
  private static final String FIELD_REF_BACK = "refBack";
  private static final String FIELD_REF_LINK = "refLink";
  private static final String FIELD_REF_LABEL = "refLabel";
  private static final String FIELD_REF_SCHEMA = "refSchema";
  private static final String FIELD_COMPUTED = "computed";
  private static final String FIELD_READONLY = "readonly";
  private static final String FIELD_LABEL = "label";
  private static final String FIELD_POSITION = "position";
  private static final String FIELD_OLD_NAME = "oldName";
  private static final String FIELD_DROP = "drop";
  private static final String FIELD_IMPORT_SCHEMA = "importSchema";
  private static final String TABLES_DIR = "tables";
  private static final String FIELD_IMPORTS = "imports";
  private static final String FIELD_SETTINGS = "settings";
  private static final String FIELD_PERMISSIONS = "permissions";
  private static final String FIELD_FIXED_SCHEMAS = "fixedSchemas";
  private static final String FIELD_SCHEMA_NAME = "schemaName";
  private static final String ROLE_VIEW = "view";
  private static final String ROLE_EDIT = "edit";
  private static final String ROLE_MANAGE = "manage";
  private static final String ROLE_OWNER = "owner";
  private static final String ROLE_VIEWER = "Viewer";
  private static final String ROLE_EDITOR = "Editor";
  private static final String ROLE_MANAGER = "Manager";
  private static final String ROLE_OWNER_VALUE = "Owner";

  private static final String FIELD_TEMPLATES = "templates";
  private static final String FIELD_SUBTYPES = "subtypes";
  private static final String FIELD_SUBTYPE = "subtype";
  private static final String FIELD_TABLES = "tables";
  private static final String FIELD_NAMESPACES = "namespaces";
  private static final String RESERVED_COLUMNS = "columns";
  private static final String TYPE_EXTENSION = "extension";
  private static final String TYPE_EXTENSION_ARRAY = "extension_array";
  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String SINGLE_FILE_FORBIDDEN_ONTOLOGIES = "ontologies";
  private static final String SINGLE_FILE_FORBIDDEN_DEMODATA = "demodata";
  private static final String SINGLE_FILE_FORBIDDEN_MIGRATIONS = "migrations";

  private static final Logger log = LoggerFactory.getLogger(Emx2Yaml.class);

  private Emx2Yaml() {}

  public static BundleResult fromBundle(Path bundlePathOrFile) throws IOException {
    if (Files.isRegularFile(bundlePathOrFile)) {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      Map<String, Object> rawYaml;
      try (InputStream inputStream = Files.newInputStream(bundlePathOrFile)) {
        rawYaml = mapper.readValue(inputStream, Map.class);
      }
      Path baseDir = bundlePathOrFile.getParent();
      Map<String, Object> yaml =
          ImportExpander.expandImports(rawYaml, baseDir, new LinkedHashSet<>());
      return parseSingleFileBundle(yaml, mapper);
    } else if (Files.isDirectory(bundlePathOrFile)) {
      return fromBundleDirectory(bundlePathOrFile);
    } else {
      throw new MolgenisException("Bundle path not found: " + bundlePathOrFile);
    }
  }

  @SuppressWarnings("unchecked")
  public static BundleResult fromBundle(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> rawYaml = mapper.readValue(inputStream, Map.class);
    ImportExpander.assertNoImports(rawYaml, "InputStream");
    return parseSingleFileBundle(rawYaml, mapper);
  }

  @SuppressWarnings("unchecked")
  private static BundleResult parseSingleFileBundle(Map<String, Object> yaml, ObjectMapper mapper) {
    String name = (String) yaml.get(FIELD_NAME);
    if (name == null) {
      throw new MolgenisException("Bundle parse error: missing required 'name' field");
    }

    for (String forbidden :
        List.of(
            SINGLE_FILE_FORBIDDEN_ONTOLOGIES,
            SINGLE_FILE_FORBIDDEN_DEMODATA,
            SINGLE_FILE_FORBIDDEN_MIGRATIONS,
            FIELD_SETTINGS)) {
      if (yaml.containsKey(forbidden)) {
        throw new MolgenisException(
            "Single-file bundle '"
                + name
                + "' must not declare '"
                + forbidden
                + "'; use the directory form for that.");
      }
    }

    Map<String, Object> rawTables = (Map<String, Object>) yaml.getOrDefault(FIELD_TABLES, Map.of());
    if (rawTables.isEmpty()) {
      throw new MolgenisException(
          "Bundle '" + name + "' must declare at least one table in 'tables:'");
    }

    Bundle bundle = mapper.convertValue(yaml, Bundle.class);
    Map<String, SubsetEntry> subsetRegistry = toSubsetEntryMap(bundle.templates(), true);
    Map<String, SubsetEntry> templateRegistry = toSubsetEntryMap(bundle.templates(), false);
    Map<String, SubsetEntry> combinedRegistry = toCombinedEntryMap(bundle.templates());
    SchemaMetadata schema = bundleToSchemaMetadata(bundle);

    BundleValidator.validate(name, combinedRegistry, schema);

    return new BundleResult(
        bundle.name(), bundle.description(), schema, subsetRegistry, templateRegistry, Map.of());
  }

  @SuppressWarnings("unchecked")
  private static BundleResult fromBundleDirectory(Path directory) throws IOException {
    Path molgenisYaml = directory.resolve(MOLGENIS_YAML);
    if (!Files.exists(molgenisYaml)) {
      throw new MolgenisException("Directory bundle missing 'molgenis.yaml' at: " + directory);
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> rawYaml;
    try (InputStream inputStream = Files.newInputStream(molgenisYaml)) {
      rawYaml = mapper.readValue(inputStream, Map.class);
    }

    Set<Path> visited = new LinkedHashSet<>();
    visited.add(molgenisYaml.toRealPath());
    Map<String, Object> yaml = ImportExpander.expandImports(rawYaml, directory, visited);

    String name = (String) yaml.get(FIELD_NAME);
    if (name == null) {
      throw new MolgenisException(
          "Bundle parse error: missing required 'name' field in " + molgenisYaml);
    }

    Map<String, String> namespaces = parseNamespaces(yaml);
    Bundle bundle = mapper.convertValue(yaml, Bundle.class);
    Map<String, SubsetEntry> subsetRegistry = toSubsetEntryMap(bundle.templates(), true);
    Map<String, SubsetEntry> templateRegistry = toSubsetEntryMap(bundle.templates(), false);
    Map<String, SubsetEntry> combinedRegistry = toCombinedEntryMap(bundle.templates());
    SchemaMetadata schema = bundleToSchemaMetadata(bundle);

    BundleValidator.validate(name, combinedRegistry, schema);

    return new BundleResult(
        bundle.name(), bundle.description(), schema, subsetRegistry, templateRegistry, namespaces);
  }

  private static void loadTableFileIntoSchema(Path yamlFile, SchemaMetadata schema)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> yaml;
    try (InputStream inputStream = Files.newInputStream(yamlFile)) {
      yaml = mapper.readValue(inputStream, Map.class);
    }
    String tableName = (String) yaml.get(FIELD_TABLE);
    if (tableName == null) {
      throw new MolgenisException("Table file missing 'table:' field: " + yamlFile);
    }
    parseNewFormatTableIntoSchema(tableName, yaml, schema);
  }

  private static Map<String, SubsetEntry> toSubsetEntryMap(
      Map<String, TemplateDef> templateMap, boolean internalOnly) {
    Map<String, SubsetEntry> result = new LinkedHashMap<>();
    for (Map.Entry<String, TemplateDef> entry : templateMap.entrySet()) {
      boolean isInternal = Boolean.TRUE.equals(entry.getValue().internal());
      if (isInternal == internalOnly) {
        TemplateDef def = entry.getValue();
        result.put(
            entry.getKey(), new SubsetEntry(entry.getKey(), def.description(), def.includes()));
      }
    }
    return result;
  }

  private static Map<String, SubsetEntry> toCombinedEntryMap(Map<String, TemplateDef> templateMap) {
    Map<String, SubsetEntry> result = new LinkedHashMap<>();
    for (Map.Entry<String, TemplateDef> entry : templateMap.entrySet()) {
      TemplateDef def = entry.getValue();
      result.put(
          entry.getKey(), new SubsetEntry(entry.getKey(), def.description(), def.includes()));
    }
    return result;
  }

  private static SchemaMetadata bundleToSchemaMetadata(Bundle bundle) {
    SchemaMetadata schema = new SchemaMetadata();
    for (Map.Entry<String, TableDef> entry : bundle.tables().entrySet()) {
      materializeTableDef(entry.getKey(), entry.getValue(), schema);
    }
    return schema;
  }

  private static void materializeTableDef(
      String tableName, TableDef tableDef, SchemaMetadata schema) {
    TableMetadata rootTable = new TableMetadata(tableName);

    if (tableDef.description() != null) {
      rootTable.setDescription(tableDef.description());
    }
    if (tableDef.semantics() != null && !tableDef.semantics().isEmpty()) {
      rootTable.setSemantics(tableDef.semantics().toArray(new String[0]));
    }
    if (tableDef.label() != null) {
      rootTable.setLabel(tableDef.label());
    }
    if (tableDef.oldName() != null) {
      rootTable.setOldName(tableDef.oldName());
    }
    if (tableDef.importSchema() != null) {
      rootTable.setImportSchema(tableDef.importSchema());
    }
    if (!tableDef.inherits().isEmpty()) {
      rootTable.setInheritNames(tableDef.inherits().toArray(new String[0]));
    }
    if (tableDef.isInternal()) {
      rootTable.setTableType(TableType.INTERNAL);
    }
    if (!tableDef.templates().isEmpty()) {
      rootTable.setSubsets(tableDef.templates().toArray(new String[0]));
    }

    Map<String, TableMetadata> subtypeTablesByName = new LinkedHashMap<>();
    for (Map.Entry<String, SubtypeDef> entry : tableDef.subtypes().entrySet()) {
      String subtypeName = entry.getKey();
      SubtypeDef subtypeDef = entry.getValue();
      TableMetadata subtypeTable = new TableMetadata(subtypeName);
      if (subtypeDef.inherits().isEmpty()) {
        subtypeTable.setInheritNames(tableName);
      } else {
        subtypeTable.setInheritNames(subtypeDef.inherits().toArray(new String[0]));
      }
      if (subtypeDef.isInternal()) {
        subtypeTable.setTableType(TableType.INTERNAL);
      }
      if (subtypeDef.description() != null) {
        subtypeTable.setDescription(subtypeDef.description());
      }
      subtypeTablesByName.put(subtypeName, subtypeTable);
    }

    Set<String> seenColumnNames = new LinkedHashSet<>();
    applyDataColumns(
        tableDef.columns(), tableName, rootTable, subtypeTablesByName, null, null, seenColumnNames);
    applySections(tableDef.sections(), tableName, rootTable, subtypeTablesByName, seenColumnNames);

    createIfAbsent(schema, rootTable);
    for (TableMetadata subtypeTable : subtypeTablesByName.values()) {
      createIfAbsent(schema, subtypeTable);
    }
  }

  private static void applyDataColumns(
      Map<String, DataColumn> columns,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> subtypeTablesByName,
      String inheritedSubtype,
      String[] inheritedSubsets,
      Set<String> seenColumnNames) {

    for (Map.Entry<String, DataColumn> entry : columns.entrySet()) {
      DataColumn col = entry.getValue() != null ? entry.getValue() : emptyDataColumn();
      applyDataColumn(
          entry.getKey(),
          col,
          tableName,
          rootTable,
          subtypeTablesByName,
          inheritedSubtype,
          inheritedSubsets,
          seenColumnNames);
    }
  }

  private static void applySections(
      Map<String, SectionDef> sections,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> subtypeTablesByName,
      Set<String> seenColumnNames) {

    for (Map.Entry<String, SectionDef> entry : sections.entrySet()) {
      SectionDef section = entry.getValue();
      String[] sectionSubsets =
          section.templates() != null && !section.templates().isEmpty()
              ? section.templates().toArray(new String[0])
              : null;
      applyDataColumns(
          section.columns(),
          tableName,
          rootTable,
          subtypeTablesByName,
          section.subtype(),
          sectionSubsets,
          seenColumnNames);
      applyHeadings(
          section.headings(),
          tableName,
          rootTable,
          subtypeTablesByName,
          section.subtype(),
          sectionSubsets,
          seenColumnNames);
    }
  }

  private static void applyHeadings(
      Map<String, HeadingDef> headings,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> subtypeTablesByName,
      String inheritedSubtype,
      String[] inheritedSubsets,
      Set<String> seenColumnNames) {

    for (Map.Entry<String, HeadingDef> entry : headings.entrySet()) {
      HeadingDef heading = entry.getValue();
      String headingSubtype = heading.subtype() != null ? heading.subtype() : inheritedSubtype;
      String[] headingSubsets =
          heading.templates() != null && !heading.templates().isEmpty()
              ? heading.templates().toArray(new String[0])
              : inheritedSubsets;
      applyDataColumns(
          heading.columns(),
          tableName,
          rootTable,
          subtypeTablesByName,
          headingSubtype,
          headingSubsets,
          seenColumnNames);
    }
  }

  @SuppressWarnings("unchecked")
  private static void applyDataColumn(
      String columnName,
      DataColumn col,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> subtypeTablesByName,
      String inheritedSubtype,
      String[] inheritedSubsets,
      Set<String> seenColumnNames) {

    if (RESERVED_COLUMNS.equals(columnName)) {
      throw new MolgenisException(
          "Table '"
              + tableName
              + "': 'columns' is a reserved name and cannot be used as a column, section, or heading name");
    }

    if (!seenColumnNames.add(columnName)) {
      throw new MolgenisException(
          "Table '"
              + tableName
              + "': duplicate column name '"
              + columnName
              + "' found in column map (column names must be unique across the full table including sections and headings)");
    }

    Column column = new Column(columnName);

    if (col.type() != null) {
      column.setType(resolveColumnType(col.type(), columnName, tableName));
    }
    if (col.key() != null) {
      column.setKey(col.key());
    }
    if (col.required() != null) {
      if (col.required() instanceof Boolean b) {
        column.setRequired(b);
      } else {
        column.setRequired(col.required().toString());
      }
    }
    if (col.defaultValue() != null) {
      column.setDefaultValue(col.defaultValue());
    }
    if (col.refTable() != null) {
      column.setRefTable(col.refTable());
    }
    if (col.refBack() != null) {
      column.setRefBack(col.refBack());
    }
    if (col.refLink() != null) {
      column.setRefLink(col.refLink());
    }
    if (col.refLabel() != null) {
      column.setRefLabel(col.refLabel());
    }
    if (col.refSchema() != null) {
      column.setRefSchemaName(col.refSchema());
    }
    if (col.computed() != null) {
      column.setComputed(col.computed());
    }
    if (col.readonly() != null) {
      column.setReadonly(col.readonly());
    }
    if (col.label() != null) {
      column.setLabel(col.label());
    }
    if (col.position() != null) {
      column.setPosition(col.position());
    }
    if (col.oldName() != null) {
      column.setOldName(col.oldName());
    }
    if (Boolean.TRUE.equals(col.drop())) {
      column.drop();
    }
    if (col.semantics() != null && !col.semantics().isEmpty()) {
      column.setSemantics(col.semantics().toArray(new String[0]));
    }
    if (col.validation() != null) {
      column.setValidation(col.validation());
    }
    if (col.visible() != null) {
      column.setVisible(col.visible());
    }
    if (col.description() != null) {
      column.setDescription(col.description());
    }

    String[] effectiveSubsets;
    if (col.templates() != null) {
      effectiveSubsets = col.templates().toArray(new String[0]);
    } else {
      effectiveSubsets = inheritedSubsets;
    }
    if (effectiveSubsets != null && effectiveSubsets.length > 0) {
      column.setSubsets(effectiveSubsets);
    }

    String effectiveSubtype = col.subtype() != null ? col.subtype() : inheritedSubtype;
    TableMetadata target =
        resolveTargetTable(effectiveSubtype, rootTable, subtypeTablesByName, tableName, columnName);
    target.add(column);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> parseNamespaces(Map<String, Object> yaml) {
    Object raw = yaml.get(FIELD_NAMESPACES);
    if (raw == null) {
      return Map.of();
    }
    Map<String, Object> rawMap = (Map<String, Object>) raw;
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
      result.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void parseNewFormatTableIntoSchema(
      String tableName, Map<String, Object> tableMap, SchemaMetadata schema) {

    TableMetadata rootTable = new TableMetadata(tableName);
    applyTableDescription(rootTable, tableMap);
    applyTableSubsets(rootTable, tableMap);

    List<String> topLevelInherits = (List<String>) tableMap.getOrDefault(FIELD_INHERITS, List.of());
    if (!topLevelInherits.isEmpty()) {
      rootTable.setInheritNames(topLevelInherits.toArray(new String[0]));
    }
    if (Boolean.TRUE.equals(tableMap.get(FIELD_INTERNAL))) {
      rootTable.setTableType(TableType.INTERNAL);
    }

    Map<String, TableMetadata> subtypeTablesByName = new LinkedHashMap<>();
    Map<String, Object> rawSubtypes =
        (Map<String, Object>) tableMap.getOrDefault(FIELD_SUBTYPES, Map.of());
    for (Map.Entry<String, Object> subtypeEntry : rawSubtypes.entrySet()) {
      String subtypeName = subtypeEntry.getKey();
      Map<String, Object> subtypeMap =
          subtypeEntry.getValue() != null
              ? (Map<String, Object>) subtypeEntry.getValue()
              : Map.of();
      TableMetadata subtypeTable = new TableMetadata(subtypeName);
      List<String> inherits = (List<String>) subtypeMap.getOrDefault(FIELD_INHERITS, List.of());
      if (inherits.isEmpty()) {
        subtypeTable.setInheritNames(tableName);
      } else {
        subtypeTable.setInheritNames(inherits.toArray(new String[0]));
      }
      if (Boolean.TRUE.equals(subtypeMap.get(FIELD_INTERNAL))) {
        subtypeTable.setTableType(TableType.INTERNAL);
      }
      applyTableDescription(subtypeTable, subtypeMap);
      applyTableSubsets(subtypeTable, subtypeMap);
      subtypeTablesByName.put(subtypeName, subtypeTable);
    }

    Map<String, Object> columnsMap =
        (Map<String, Object>) tableMap.getOrDefault(FIELD_COLUMNS, Map.of());

    Set<String> seenColumnNames = new LinkedHashSet<>();
    parseColumnsAtDepth(
        columnsMap, 0, tableName, rootTable, subtypeTablesByName, null, null, seenColumnNames);

    createIfAbsent(schema, rootTable);
    for (TableMetadata subtypeTable : subtypeTablesByName.values()) {
      createIfAbsent(schema, subtypeTable);
    }
  }

  private static void createIfAbsent(SchemaMetadata schema, TableMetadata table) {
    if (schema.getTableMetadata(table.getTableName()) == null) {
      schema.create(table);
    }
  }

  @SuppressWarnings("unchecked")
  private static void parseColumnsAtDepth(
      Map<String, Object> columnsMap,
      int depth,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> subtypeTablesByName,
      String inheritedSubtype,
      String[] inheritedSubsets,
      Set<String> seenColumnNames) {

    for (Map.Entry<String, Object> columnEntry : columnsMap.entrySet()) {
      String columnKey = columnEntry.getKey();

      if (RESERVED_COLUMNS.equals(columnKey)) {
        throw new MolgenisException(
            "Table '"
                + tableName
                + "': 'columns' is a reserved name and cannot be used as a column, section, or heading name");
      }

      Map<String, Object> columnAttrs =
          columnEntry.getValue() != null ? (Map<String, Object>) columnEntry.getValue() : Map.of();

      Map<String, Object> nestedColumns = (Map<String, Object>) columnAttrs.get(FIELD_COLUMNS);
      boolean hasNestedColumns = nestedColumns != null;
      boolean hasTypeHeading = "heading".equals(columnAttrs.get(FIELD_TYPE));

      if (hasNestedColumns) {
        if (depth >= 2) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "', entry '"
                  + columnKey
                  + "': nesting depth exceeded (max 2: table → section → heading → columns)");
        }

        if (columnAttrs.containsKey(FIELD_SEMANTICS)) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "', "
                  + (depth == 0 ? "section" : "heading")
                  + " '"
                  + columnKey
                  + "': 'semantics:' is not allowed on a section or heading; set it per data column");
        }

        String containerSubtype =
            columnAttrs.containsKey(FIELD_SUBTYPE)
                ? (String) columnAttrs.get(FIELD_SUBTYPE)
                : inheritedSubtype;
        String[] containerSubsets;
        if (columnAttrs.containsKey(FIELD_TEMPLATES)) {
          containerSubsets = toStringArray((List<String>) columnAttrs.get(FIELD_TEMPLATES));
        } else if (columnAttrs.containsKey("subsets")) {
          containerSubsets = toStringArray((List<String>) columnAttrs.get("subsets"));
        } else {
          containerSubsets = inheritedSubsets;
        }

        parseColumnsAtDepth(
            nestedColumns,
            depth + 1,
            tableName,
            rootTable,
            subtypeTablesByName,
            containerSubtype,
            containerSubsets,
            seenColumnNames);
      } else if (hasTypeHeading) {
        if (columnAttrs.containsKey(FIELD_SEMANTICS)) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "', heading '"
                  + columnKey
                  + "': 'semantics:' is not allowed on a heading; set it per data column");
        }
        Column headingCol = new Column(columnKey);
        headingCol.setType(ColumnType.HEADING);
        applyCommonColumnAttributes(headingCol, columnAttrs);
        if (inheritedSubsets != null) {
          headingCol.setSubsets(inheritedSubsets);
        }
        TableMetadata target =
            resolveTargetTable(
                inheritedSubtype, rootTable, subtypeTablesByName, tableName, columnKey);
        target.add(headingCol);
      } else {
        if (!seenColumnNames.add(columnKey)) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "': duplicate column name '"
                  + columnKey
                  + "' found in column map (column names must be unique across the full table including sections and headings)");
        }

        Column column = new Column(columnKey);
        String typeStr = (String) columnAttrs.get(FIELD_TYPE);
        if (typeStr != null) {
          column.setType(resolveColumnType(typeStr, columnKey, tableName));
        }

        applyCommonColumnAttributes(column, columnAttrs);

        String[] effectiveSubsets;
        if (columnAttrs.containsKey(FIELD_TEMPLATES)) {
          effectiveSubsets = toStringArray((List<String>) columnAttrs.get(FIELD_TEMPLATES));
        } else if (columnAttrs.containsKey("subsets")) {
          effectiveSubsets = toStringArray((List<String>) columnAttrs.get("subsets"));
        } else {
          effectiveSubsets = inheritedSubsets;
        }
        if (effectiveSubsets != null && effectiveSubsets.length > 0) {
          column.setSubsets(effectiveSubsets);
        }

        String effectiveSubtype =
            columnAttrs.containsKey(FIELD_SUBTYPE)
                ? (String) columnAttrs.get(FIELD_SUBTYPE)
                : inheritedSubtype;

        TableMetadata target =
            resolveTargetTable(
                effectiveSubtype, rootTable, subtypeTablesByName, tableName, columnKey);
        target.add(column);
      }
    }
  }

  private static TableMetadata resolveTargetTable(
      String subtypeName,
      TableMetadata rootTable,
      Map<String, TableMetadata> subtypeTablesByName,
      String tableName,
      String columnKey) {
    if (subtypeName == null) {
      return rootTable;
    }
    TableMetadata target = subtypeTablesByName.get(subtypeName);
    if (target == null) {
      throw new MolgenisException(
          "Table '"
              + tableName
              + "', column '"
              + columnKey
              + "': references unknown subtype '"
              + subtypeName
              + "'");
    }
    return target;
  }

  private static ColumnType resolveColumnType(String typeStr, String columnKey, String tableName) {
    String normalized = typeStr.toLowerCase().replace(" ", "_");
    if (SchemaMetadataToBundle.TYPE_SUBTYPE.equals(normalized)) {
      return ColumnType.EXTENSION;
    }
    if (SchemaMetadataToBundle.TYPE_SUBTYPE_ARRAY.equals(normalized)) {
      return ColumnType.EXTENSION_ARRAY;
    }
    if (TYPE_EXTENSION.equals(normalized)) {
      return ColumnType.EXTENSION;
    }
    if (TYPE_EXTENSION_ARRAY.equals(normalized)) {
      return ColumnType.EXTENSION_ARRAY;
    }
    try {
      return ColumnType.valueOf(normalized.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new MolgenisException(
          "Table '" + tableName + "', column '" + columnKey + "': unknown type '" + typeStr + "'");
    }
  }

  @SuppressWarnings("unchecked")
  private static void applyCommonColumnAttributes(Column column, Map<String, Object> attrs) {
    Object key = attrs.get(FIELD_KEY);
    if (key != null) {
      column.setKey(((Number) key).intValue());
    }

    Object required = attrs.get(FIELD_REQUIRED);
    if (required != null) {
      if (required instanceof Boolean) {
        column.setRequired((Boolean) required);
      } else {
        column.setRequired(required.toString());
      }
    }

    String defaultValue = (String) attrs.get(FIELD_DEFAULT_VALUE);
    if (defaultValue != null) {
      column.setDefaultValue(defaultValue);
    }

    String refTable = (String) attrs.get(FIELD_REF_TABLE);
    if (refTable != null) {
      column.setRefTable(refTable);
    }

    String refBack = (String) attrs.get(FIELD_REF_BACK);
    if (refBack != null) {
      column.setRefBack(refBack);
    }

    String refLink = (String) attrs.get(FIELD_REF_LINK);
    if (refLink != null) {
      column.setRefLink(refLink);
    }

    String refLabel = (String) attrs.get(FIELD_REF_LABEL);
    if (refLabel != null) {
      column.setRefLabel(refLabel);
    }

    String refSchema = (String) attrs.get(FIELD_REF_SCHEMA);
    if (refSchema != null) {
      column.setRefSchemaName(refSchema);
    }

    String computed = (String) attrs.get(FIELD_COMPUTED);
    if (computed != null) {
      column.setComputed(computed);
    }

    Object readonly = attrs.get(FIELD_READONLY);
    if (readonly != null) {
      if (readonly instanceof Boolean) {
        column.setReadonly((Boolean) readonly);
      } else {
        column.setReadonly(Boolean.parseBoolean(readonly.toString()));
      }
    }

    String label = (String) attrs.get(FIELD_LABEL);
    if (label != null) {
      column.setLabel(label);
    }

    Object position = attrs.get(FIELD_POSITION);
    if (position != null) {
      column.setPosition(((Number) position).intValue());
    }

    String oldName = (String) attrs.get(FIELD_OLD_NAME);
    if (oldName != null) {
      column.setOldName(oldName);
    }

    Object drop = attrs.get(FIELD_DROP);
    if (Boolean.TRUE.equals(drop)) {
      column.drop();
    }

    Object semantics = attrs.get(FIELD_SEMANTICS);
    if (semantics instanceof List) {
      column.setSemantics(((List<String>) semantics).toArray(new String[0]));
    } else if (semantics instanceof String) {
      column.setSemantics((String) semantics);
    }

    String validation = (String) attrs.get(FIELD_VALIDATION);
    if (validation != null) {
      column.setValidation(validation);
    }

    String visible = (String) attrs.get(FIELD_VISIBLE);
    if (visible != null) {
      column.setVisible(visible);
    }

    String description = (String) attrs.get(FIELD_DESCRIPTION);
    if (description != null) {
      column.setDescription(description);
    }
  }

  @SuppressWarnings("unchecked")
  private static void applyTableSubsets(TableMetadata table, Map<String, Object> source) {
    Object subsets = source.get(FIELD_TEMPLATES);
    if (subsets == null) {
      subsets = source.get("subsets");
    }
    if (subsets instanceof List) {
      table.setSubsets(((List<String>) subsets).toArray(new String[0]));
    } else if (subsets instanceof String) {
      table.setSubsets((String) subsets);
    }
  }

  private static DataColumn emptyDataColumn() {
    return DataColumn.empty();
  }

  private static String[] toStringArray(List<String> list) {
    if (list == null || list.isEmpty()) {
      return new String[0];
    }
    return list.toArray(new String[0]);
  }

  public static class BundleResult {
    private final String name;
    private final String description;
    private final SchemaMetadata schema;
    private final Map<String, SubsetEntry> subsetRegistry;
    private final Map<String, SubsetEntry> templateRegistry;
    private final Map<String, String> namespaces;

    public BundleResult(
        String name,
        String description,
        SchemaMetadata schema,
        Map<String, SubsetEntry> subsetRegistry,
        Map<String, SubsetEntry> templateRegistry,
        Map<String, String> namespaces) {
      this.name = name;
      this.description = description;
      this.schema = schema;
      this.subsetRegistry = subsetRegistry;
      this.templateRegistry = templateRegistry;
      this.namespaces = namespaces;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public SchemaMetadata getSchema() {
      return schema;
    }

    public Map<String, SubsetEntry> getSubsetRegistry() {
      return subsetRegistry;
    }

    public Map<String, SubsetEntry> getTemplateRegistry() {
      return templateRegistry;
    }

    public Map<String, String> getNamespaces() {
      return namespaces;
    }

    public BundleContext toBundleContext() {
      return new BundleContext(name, description, schema, subsetRegistry, templateRegistry);
    }
  }

  public static void toBundleDirectory(
      SchemaMetadata schema, String bundleName, String bundleDescription, Path directory)
      throws IOException {
    Path tablesDir = directory.resolve(TABLES_DIR);
    Files.createDirectories(tablesDir);

    Bundle bundle =
        SchemaMetadataToBundle.convertWithAutoRegistry(schema, bundleName, bundleDescription);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    Map<String, Object> molgenisYamlDoc = buildMolgenisYamlDoc(bundle, TABLES_DIR + "/");
    Files.writeString(directory.resolve(MOLGENIS_YAML), mapper.writeValueAsString(molgenisYamlDoc));

    for (Map.Entry<String, TableDef> entry : bundle.tables().entrySet()) {
      Map<String, Object> tableDoc = buildTableDocument(entry.getKey(), entry.getValue());
      Files.writeString(
          tablesDir.resolve(entry.getKey() + ".yaml"), mapper.writeValueAsString(tableDoc));
    }
  }

  public static void toBundleSingleFile(
      SchemaMetadata schema, String bundleName, String bundleDescription, Path outputFile)
      throws IOException {
    Bundle bundle =
        SchemaMetadataToBundle.convertWithAutoRegistry(schema, bundleName, bundleDescription);
    Map<String, Object> doc = buildSingleFileDoc(bundle);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Files.writeString(outputFile, mapper.writeValueAsString(doc));
  }

  private static Map<String, Object> buildMolgenisYamlDoc(Bundle bundle, String tablesImportPath) {
    Map<String, Object> doc = new LinkedHashMap<>();
    putIfNotNull(doc, FIELD_NAME, bundle.name());
    putIfNotNull(doc, FIELD_DESCRIPTION, bundle.description());
    if (!bundle.namespaces().isEmpty()) {
      doc.put(FIELD_NAMESPACES, new LinkedHashMap<>(bundle.namespaces()));
    }
    if (!bundle.templates().isEmpty()) {
      doc.put(FIELD_TEMPLATES, buildTemplateDefMap(bundle.templates()));
    }
    doc.put("imports", List.of(tablesImportPath));
    return doc;
  }

  private static Map<String, Object> buildSingleFileDoc(Bundle bundle) {
    Map<String, Object> doc = new LinkedHashMap<>();
    putIfNotNull(doc, FIELD_NAME, bundle.name());
    putIfNotNull(doc, FIELD_DESCRIPTION, bundle.description());
    if (!bundle.namespaces().isEmpty()) {
      doc.put(FIELD_NAMESPACES, new LinkedHashMap<>(bundle.namespaces()));
    }
    if (!bundle.templates().isEmpty()) {
      doc.put(FIELD_TEMPLATES, buildTemplateDefMap(bundle.templates()));
    }
    Map<String, Object> tablesMap = new LinkedHashMap<>();
    for (Map.Entry<String, TableDef> entry : bundle.tables().entrySet()) {
      tablesMap.put(entry.getKey(), buildTableEntry(entry.getValue()));
    }
    doc.put(FIELD_TABLES, tablesMap);
    return doc;
  }

  private static Map<String, Object> buildTableDocument(String tableName, TableDef table) {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_TABLE, tableName);
    putIfNotNull(doc, FIELD_DESCRIPTION, table.description());
    if (!table.inherits().isEmpty()) {
      doc.put(FIELD_INHERITS, table.inherits());
    }
    if (table.isInternal()) {
      doc.put(FIELD_INTERNAL, true);
    }
    putIfNotNull(doc, FIELD_SEMANTICS, table.semantics());
    putIfNotNull(doc, FIELD_LABEL, table.label());
    putIfNotNull(doc, FIELD_OLD_NAME, table.oldName());
    putIfNotNull(doc, "importSchema", table.importSchema());
    if (!table.templates().isEmpty()) {
      doc.put(FIELD_TEMPLATES, table.templates());
    }
    if (!table.subtypes().isEmpty()) {
      doc.put(FIELD_SUBTYPES, buildSubtypeDefMap(table.subtypes(), tableName));
    }
    if (!table.columns().isEmpty()) {
      doc.put(FIELD_COLUMNS, buildDataColumnMap(table.columns()));
    }
    if (!table.sections().isEmpty()) {
      doc.put("sections", buildSectionDefMap(table.sections()));
    }
    return doc;
  }

  private static Map<String, Object> buildTableEntry(TableDef table) {
    Map<String, Object> entry = new LinkedHashMap<>();
    putIfNotNull(entry, FIELD_DESCRIPTION, table.description());
    if (!table.inherits().isEmpty()) {
      entry.put(FIELD_INHERITS, table.inherits());
    }
    if (table.isInternal()) {
      entry.put(FIELD_INTERNAL, true);
    }
    putIfNotNull(entry, FIELD_SEMANTICS, table.semantics());
    putIfNotNull(entry, FIELD_LABEL, table.label());
    putIfNotNull(entry, FIELD_OLD_NAME, table.oldName());
    putIfNotNull(entry, "importSchema", table.importSchema());
    if (!table.templates().isEmpty()) {
      entry.put(FIELD_TEMPLATES, table.templates());
    }
    if (!table.subtypes().isEmpty()) {
      entry.put(FIELD_SUBTYPES, buildSubtypeDefMap(table.subtypes(), null));
    }
    if (!table.columns().isEmpty()) {
      entry.put(FIELD_COLUMNS, buildDataColumnMap(table.columns()));
    }
    if (!table.sections().isEmpty()) {
      entry.put("sections", buildSectionDefMap(table.sections()));
    }
    return entry;
  }

  private static Map<String, Object> buildTemplateDefMap(Map<String, TemplateDef> defMap) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, TemplateDef> entry : defMap.entrySet()) {
      TemplateDef def = entry.getValue();
      Map<String, Object> defDoc = new LinkedHashMap<>();
      putIfNotNull(defDoc, FIELD_DESCRIPTION, def.description());
      if (!def.includes().isEmpty()) {
        defDoc.put("includes", def.includes());
      }
      if (Boolean.TRUE.equals(def.internal())) {
        defDoc.put(FIELD_INTERNAL, true);
      }
      result.put(entry.getKey(), defDoc.isEmpty() ? null : defDoc);
    }
    return result;
  }

  private static Map<String, Object> buildSubtypeDefMap(
      Map<String, SubtypeDef> subtypes, String defaultParent) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, SubtypeDef> entry : subtypes.entrySet()) {
      SubtypeDef def = entry.getValue();
      Map<String, Object> defDoc = new LinkedHashMap<>();
      putIfNotNull(defDoc, FIELD_DESCRIPTION, def.description());
      if (!def.inherits().isEmpty()
          && !(def.inherits().size() == 1 && entry.getKey().equals(defaultParent))) {
        defDoc.put(FIELD_INHERITS, def.inherits());
      }
      if (def.isInternal()) {
        defDoc.put(FIELD_INTERNAL, true);
      }
      result.put(entry.getKey(), defDoc.isEmpty() ? null : defDoc);
    }
    return result;
  }

  private static Map<String, Object> buildDataColumnMap(Map<String, DataColumn> columns) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, DataColumn> entry : columns.entrySet()) {
      result.put(entry.getKey(), buildDataColumnEntry(entry.getValue()));
    }
    return result;
  }

  private static Map<String, Object> buildSectionDefMap(Map<String, SectionDef> sections) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, SectionDef> entry : sections.entrySet()) {
      result.put(entry.getKey(), buildSectionEntry(entry.getValue()));
    }
    return result;
  }

  private static Map<String, Object> buildSectionEntry(SectionDef section) {
    Map<String, Object> entry = new LinkedHashMap<>();
    putIfNotNull(entry, FIELD_SUBTYPE, section.subtype());
    if (!section.templates().isEmpty()) {
      entry.put(FIELD_TEMPLATES, section.templates());
    }
    if (!section.columns().isEmpty()) {
      entry.put(FIELD_COLUMNS, buildDataColumnMap(section.columns()));
    }
    if (!section.headings().isEmpty()) {
      entry.put("headings", buildHeadingDefMap(section.headings()));
    }
    return entry;
  }

  private static Map<String, Object> buildHeadingDefMap(Map<String, HeadingDef> headings) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, HeadingDef> entry : headings.entrySet()) {
      result.put(entry.getKey(), buildHeadingEntry(entry.getValue()));
    }
    return result;
  }

  private static Map<String, Object> buildHeadingEntry(HeadingDef heading) {
    Map<String, Object> entry = new LinkedHashMap<>();
    putIfNotNull(entry, FIELD_SUBTYPE, heading.subtype());
    if (!heading.templates().isEmpty()) {
      entry.put(FIELD_TEMPLATES, heading.templates());
    }
    if (!heading.columns().isEmpty()) {
      entry.put(FIELD_COLUMNS, buildDataColumnMap(heading.columns()));
    }
    return entry;
  }

  private static Map<String, Object> buildDataColumnEntry(DataColumn col) {
    Map<String, Object> entry = new LinkedHashMap<>();
    putIfNotNull(entry, FIELD_TYPE, col.type());
    if (col.key() != null && col.key() > 0) {
      entry.put(FIELD_KEY, col.key());
    }
    if (col.required() != null) {
      entry.put(FIELD_REQUIRED, col.required());
    }
    putIfNotNull(entry, FIELD_DEFAULT_VALUE, col.defaultValue());
    putIfNotNull(entry, FIELD_REF_TABLE, col.refTable());
    putIfNotNull(entry, FIELD_REF_BACK, col.refBack());
    putIfNotNull(entry, FIELD_REF_LINK, col.refLink());
    putIfNotNull(entry, FIELD_REF_LABEL, col.refLabel());
    putIfNotNull(entry, FIELD_REF_SCHEMA, col.refSchema());
    putIfNotNull(entry, FIELD_DESCRIPTION, col.description());
    if (col.semantics() != null && !col.semantics().isEmpty()) {
      entry.put(FIELD_SEMANTICS, col.semantics());
    }
    if (col.templates() != null && !col.templates().isEmpty()) {
      entry.put(FIELD_TEMPLATES, col.templates());
    }
    putIfNotNull(entry, FIELD_VALIDATION, col.validation());
    putIfNotNull(entry, FIELD_VISIBLE, col.visible());
    putIfNotNull(entry, FIELD_COMPUTED, col.computed());
    if (Boolean.TRUE.equals(col.readonly())) {
      entry.put(FIELD_READONLY, true);
    }
    putIfNotNull(entry, FIELD_LABEL, col.label());
    putIfNotNull(entry, FIELD_SUBTYPE, col.subtype());
    if (col.position() != null && col.position() > 0) {
      entry.put(FIELD_POSITION, col.position());
    }
    putIfNotNull(entry, FIELD_OLD_NAME, col.oldName());
    if (Boolean.TRUE.equals(col.drop())) {
      entry.put(FIELD_DROP, true);
    }
    return entry;
  }

  private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  static boolean isValidSubsetIdentifier(String id) {
    return TemplateNameNormalizer.isValidIdentifier(id);
  }

  @SuppressWarnings("unchecked")
  public static TemplateResult fromYamlTemplate(Path templateFile) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> yaml;
    try (InputStream inputStream = Files.newInputStream(templateFile)) {
      yaml = mapper.readValue(inputStream, Map.class);
    }

    String name = (String) yaml.get(FIELD_NAME);
    if (name == null) {
      throw new MolgenisException("YAML template parse error: missing required 'name' field");
    }
    String description = (String) yaml.get(FIELD_DESCRIPTION);

    Path baseDir = templateFile.getParent();
    SchemaMetadata schema = new SchemaMetadata();
    List<String> imports = (List<String>) yaml.getOrDefault(FIELD_IMPORTS, List.of());
    for (String importEntry : imports) {
      for (Path yamlFile : resolveImport(baseDir, importEntry)) {
        loadTableFileIntoSchema(yamlFile, schema);
      }
    }

    if (yaml.containsKey("profiles")) {
      throw new MolgenisException(
          "YAML template parse error: 'profiles' is no longer supported. Use 'activeSubsets' instead.");
    }
    List<String> activeSubsets = (List<String>) yaml.getOrDefault(FIELD_ACTIVE_SUBSETS, List.of());

    Map<String, String> settings = new LinkedHashMap<>();
    Map<String, Object> rawSettings =
        (Map<String, Object>) yaml.getOrDefault(FIELD_SETTINGS, Map.of());
    for (Map.Entry<String, Object> entry : rawSettings.entrySet()) {
      settings.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
    }

    Map<String, String> permissions = new LinkedHashMap<>();
    Map<String, Object> rawPermissions =
        (Map<String, Object>) yaml.getOrDefault(FIELD_PERMISSIONS, Map.of());
    for (Map.Entry<String, Object> entry : rawPermissions.entrySet()) {
      String role = mapRole(entry.getKey());
      if (role != null) {
        permissions.put(entry.getValue().toString(), role);
      }
    }

    List<FixedSchema> fixedSchemas = new ArrayList<>();
    List<Map<String, Object>> rawFixedSchemas =
        (List<Map<String, Object>>) yaml.getOrDefault(FIELD_FIXED_SCHEMAS, List.of());
    for (Map<String, Object> rawFixed : rawFixedSchemas) {
      String schemaName = (String) rawFixed.get(FIELD_SCHEMA_NAME);
      String fixedDescription = (String) rawFixed.get(FIELD_DESCRIPTION);
      SchemaMetadata fixedSchema = new SchemaMetadata();
      List<String> fixedImports = (List<String>) rawFixed.getOrDefault(FIELD_IMPORTS, List.of());
      for (String importEntry : fixedImports) {
        for (Path yamlFile : resolveImport(baseDir, importEntry)) {
          loadTableFileIntoSchema(yamlFile, fixedSchema);
        }
      }
      Map<String, String> fixedPermissions = new LinkedHashMap<>();
      Map<String, Object> rawFixedPermissions =
          (Map<String, Object>) rawFixed.getOrDefault(FIELD_PERMISSIONS, Map.of());
      for (Map.Entry<String, Object> entry : rawFixedPermissions.entrySet()) {
        String role = mapRole(entry.getKey());
        if (role != null) {
          fixedPermissions.put(entry.getValue().toString(), role);
        }
      }
      fixedSchemas.add(
          new FixedSchema(schemaName, fixedDescription, fixedSchema, fixedPermissions));
    }

    return new TemplateResult(
        name, description, schema, activeSubsets, settings, permissions, fixedSchemas);
  }

  private static List<Path> resolveImport(Path baseDir, String importEntry) throws IOException {
    String trimmedEntry =
        importEntry.endsWith("/")
            ? importEntry.substring(0, importEntry.length() - 1)
            : importEntry;
    if (trimmedEntry.endsWith("/*")) {
      trimmedEntry = trimmedEntry.substring(0, trimmedEntry.length() - 2);
    }
    Path resolved = baseDir.resolve(trimmedEntry).normalize();
    if (Files.isDirectory(resolved)) {
      List<Path> result = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolved, "*.yaml")) {
        for (Path path : stream) {
          result.add(path);
        }
      }
      return result;
    } else {
      return List.of(resolved);
    }
  }

  private static String mapRole(String keyword) {
    return switch (keyword) {
      case ROLE_VIEW -> ROLE_VIEWER;
      case ROLE_EDIT -> ROLE_EDITOR;
      case ROLE_MANAGE -> ROLE_MANAGER;
      case ROLE_OWNER -> ROLE_OWNER_VALUE;
      default -> null;
    };
  }

  @SuppressWarnings("unchecked")
  public static String toYamlTemplate(TemplateResult result) throws IOException {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_NAME, result.getName());
    if (result.getDescription() != null) {
      doc.put(FIELD_DESCRIPTION, result.getDescription());
    }
    if (!result.getActiveSubsets().isEmpty()) {
      doc.put(FIELD_ACTIVE_SUBSETS, result.getActiveSubsets());
    }
    if (!result.getSettings().isEmpty()) {
      doc.put(FIELD_SETTINGS, result.getSettings());
    }
    if (!result.getPermissions().isEmpty()) {
      Map<String, String> permissionsOut = new LinkedHashMap<>();
      for (Map.Entry<String, String> entry : result.getPermissions().entrySet()) {
        String keyword = reverseMapRole(entry.getValue());
        if (keyword != null) {
          permissionsOut.put(keyword, entry.getKey());
        }
      }
      doc.put(FIELD_PERMISSIONS, permissionsOut);
    }
    if (!result.getFixedSchemas().isEmpty()) {
      List<Map<String, Object>> fixedList = new ArrayList<>();
      for (FixedSchema fixed : result.getFixedSchemas()) {
        Map<String, Object> fixedMap = new LinkedHashMap<>();
        fixedMap.put(FIELD_SCHEMA_NAME, fixed.getSchemaName());
        if (fixed.getDescription() != null) {
          fixedMap.put(FIELD_DESCRIPTION, fixed.getDescription());
        }
        if (!fixed.getPermissions().isEmpty()) {
          Map<String, String> permissionsOut = new LinkedHashMap<>();
          for (Map.Entry<String, String> entry : fixed.getPermissions().entrySet()) {
            String keyword = reverseMapRole(entry.getValue());
            if (keyword != null) {
              permissionsOut.put(keyword, entry.getKey());
            }
          }
          fixedMap.put(FIELD_PERMISSIONS, permissionsOut);
        }
        fixedList.add(fixedMap);
      }
      doc.put(FIELD_FIXED_SCHEMAS, fixedList);
    }
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.writeValueAsString(doc);
  }

  private static String reverseMapRole(String role) {
    return switch (role) {
      case ROLE_VIEWER -> ROLE_VIEW;
      case ROLE_EDITOR -> ROLE_EDIT;
      case ROLE_MANAGER -> ROLE_MANAGE;
      case ROLE_OWNER_VALUE -> ROLE_OWNER;
      default -> null;
    };
  }

  public static class TemplateResult {
    private final String name;
    private final String description;
    private final SchemaMetadata schema;
    private final List<String> activeSubsets;
    private final Map<String, String> settings;
    private final Map<String, String> permissions;
    private final List<FixedSchema> fixedSchemas;

    public TemplateResult(
        String name,
        String description,
        SchemaMetadata schema,
        List<String> activeSubsets,
        Map<String, String> settings,
        Map<String, String> permissions,
        List<FixedSchema> fixedSchemas) {
      this.name = name;
      this.description = description;
      this.schema = schema;
      this.activeSubsets = activeSubsets;
      this.settings = settings;
      this.permissions = permissions;
      this.fixedSchemas = fixedSchemas;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public SchemaMetadata getSchema() {
      return schema;
    }

    public List<String> getActiveSubsets() {
      return activeSubsets;
    }

    public Map<String, String> getSettings() {
      return settings;
    }

    public Map<String, String> getPermissions() {
      return permissions;
    }

    public List<FixedSchema> getFixedSchemas() {
      return fixedSchemas;
    }
  }

  public static class FixedSchema {
    private final String schemaName;
    private final String description;
    private final SchemaMetadata schema;
    private final Map<String, String> permissions;

    public FixedSchema(
        String schemaName,
        String description,
        SchemaMetadata schema,
        Map<String, String> permissions) {
      this.schemaName = schemaName;
      this.description = description;
      this.schema = schema;
      this.permissions = permissions;
    }

    public String getSchemaName() {
      return schemaName;
    }

    public String getDescription() {
      return description;
    }

    public SchemaMetadata getSchema() {
      return schema;
    }

    public Map<String, String> getPermissions() {
      return permissions;
    }
  }

  @SuppressWarnings("unchecked")
  private static void applyTableDescription(TableMetadata table, Map<String, Object> source) {
    String description = (String) source.get(FIELD_DESCRIPTION);
    if (description != null) {
      table.setDescription(description);
    }
    Object semantics = source.get(FIELD_SEMANTICS);
    if (semantics instanceof List) {
      List<String> semList = (List<String>) semantics;
      table.setSemantics(semList.toArray(new String[0]));
    } else if (semantics instanceof String) {
      table.setSemantics((String) semantics);
    }
    String label = (String) source.get(FIELD_LABEL);
    if (label != null) {
      table.setLabel(label);
    }
    String oldName = (String) source.get(FIELD_OLD_NAME);
    if (oldName != null) {
      table.setOldName(oldName);
    }
    String importSchema = (String) source.get(FIELD_IMPORT_SCHEMA);
    if (importSchema != null) {
      table.setImportSchema(importSchema);
    }
  }
}
