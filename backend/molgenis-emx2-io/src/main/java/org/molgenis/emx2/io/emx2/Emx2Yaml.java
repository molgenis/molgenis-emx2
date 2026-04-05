package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.molgenis.emx2.*;
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

  private static final String FIELD_SUBSETS = "subsets";
  private static final String FIELD_SUBTYPES = "subtypes";
  private static final String FIELD_SUBTYPE = "subtype";
  private static final String FIELD_TABLES = "tables";
  private static final String FIELD_NAMESPACES = "namespaces";
  private static final String FIELD_TEMPLATES = "templates";
  private static final String FIELD_INCLUDES = "includes";
  private static final String RESERVED_COLUMNS = "columns";
  private static final String TYPE_SUBTYPE = "subtype";
  private static final String TYPE_SUBTYPE_ARRAY = "subtype_array";
  private static final String TYPE_EXTENSION = "extension";
  private static final String TYPE_EXTENSION_ARRAY = "extension_array";
  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String SINGLE_FILE_FORBIDDEN_ONTOLOGIES = "ontologies";
  private static final String SINGLE_FILE_FORBIDDEN_DEMODATA = "demodata";
  private static final String SINGLE_FILE_FORBIDDEN_MIGRATIONS = "migrations";

  private static final Logger log = LoggerFactory.getLogger(Emx2Yaml.class);

  private Emx2Yaml() {
    // hidden
  }

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
      return parseSingleFileBundle(yaml);
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
    return parseSingleFileBundle(rawYaml);
  }

  @SuppressWarnings("unchecked")
  private static BundleResult parseSingleFileBundle(Map<String, Object> yaml) {
    String name = (String) yaml.get(FIELD_NAME);
    if (name == null) {
      throw new MolgenisException("Bundle parse error: missing required 'name' field");
    }
    String description = (String) yaml.get(FIELD_DESCRIPTION);

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

    // TODO: parse namespaces: here (see phase9 plan roadmap)
    Map<String, SubsetEntry> subsetRegistry = parseSubsetRegistry(yaml);
    Map<String, SubsetEntry> templateRegistry = parseTemplateRegistry(yaml);

    SchemaMetadata schema = new SchemaMetadata();
    for (Map.Entry<String, Object> tableEntry : rawTables.entrySet()) {
      String tableName = tableEntry.getKey();
      Map<String, Object> tableMap =
          tableEntry.getValue() != null ? (Map<String, Object>) tableEntry.getValue() : Map.of();
      parseNewFormatTableIntoSchema(tableName, tableMap, schema);
    }

    BundleValidator.validate(name, subsetRegistry, templateRegistry, schema);

    return new BundleResult(name, description, schema, subsetRegistry, templateRegistry, Map.of());
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
    String description = (String) yaml.get(FIELD_DESCRIPTION);

    Map<String, String> namespaces = parseNamespaces(yaml);
    Map<String, SubsetEntry> subsetRegistry = parseSubsetRegistry(yaml);
    Map<String, SubsetEntry> templateRegistry = parseTemplateRegistry(yaml);

    SchemaMetadata schema = new SchemaMetadata();
    Map<String, Object> tablesMap = (Map<String, Object>) yaml.getOrDefault(FIELD_TABLES, Map.of());
    for (Map.Entry<String, Object> tableEntry : tablesMap.entrySet()) {
      String tableName = tableEntry.getKey();
      Map<String, Object> tableMap =
          tableEntry.getValue() != null ? (Map<String, Object>) tableEntry.getValue() : Map.of();
      parseNewFormatTableIntoSchema(tableName, tableMap, schema);
    }

    BundleValidator.validate(name, subsetRegistry, templateRegistry, schema);

    return new BundleResult(
        name, description, schema, subsetRegistry, templateRegistry, namespaces);
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
  private static Map<String, SubsetEntry> parseSubsetRegistry(Map<String, Object> yaml) {
    Object raw = yaml.get(FIELD_SUBSETS);
    if (raw == null) {
      return new LinkedHashMap<>();
    }
    Map<String, Object> rawMap = (Map<String, Object>) raw;
    Map<String, SubsetEntry> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
      String id = entry.getKey();
      Map<String, Object> entryMap =
          entry.getValue() != null ? (Map<String, Object>) entry.getValue() : Map.of();
      String desc = (String) entryMap.get(FIELD_DESCRIPTION);
      List<String> includes = (List<String>) entryMap.getOrDefault(FIELD_INCLUDES, List.of());
      result.put(id, new SubsetEntry(id, desc, includes));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, SubsetEntry> parseTemplateRegistry(Map<String, Object> yaml) {
    Object raw = yaml.get(FIELD_TEMPLATES);
    if (raw == null) {
      return new LinkedHashMap<>();
    }
    Map<String, Object> rawMap = (Map<String, Object>) raw;
    Map<String, SubsetEntry> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
      String id = entry.getKey();
      Map<String, Object> entryMap =
          entry.getValue() != null ? (Map<String, Object>) entry.getValue() : Map.of();
      String desc = (String) entryMap.get(FIELD_DESCRIPTION);
      List<String> includes = (List<String>) entryMap.getOrDefault(FIELD_INCLUDES, List.of());
      result.put(id, new SubsetEntry(id, desc, includes));
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
        String[] containerSubsets =
            columnAttrs.containsKey(FIELD_SUBSETS)
                ? toStringArray((List<String>) columnAttrs.get(FIELD_SUBSETS))
                : inheritedSubsets;

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
        if (columnAttrs.containsKey(FIELD_SUBSETS)) {
          effectiveSubsets = toStringArray((List<String>) columnAttrs.get(FIELD_SUBSETS));
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
    if (TYPE_SUBTYPE.equals(normalized)) {
      return ColumnType.EXTENSION;
    }
    if (TYPE_SUBTYPE_ARRAY.equals(normalized)) {
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
    Object subsets = source.get(FIELD_SUBSETS);
    if (subsets instanceof List) {
      table.setSubsets(((List<String>) subsets).toArray(new String[0]));
    } else if (subsets instanceof String) {
      table.setSubsets((String) subsets);
    }
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

    Map<String, Object> root = new LinkedHashMap<>();
    root.put(FIELD_NAME, bundleName);
    if (bundleDescription != null) {
      root.put(FIELD_DESCRIPTION, bundleDescription);
    }

    Set<String> allSubsets = collectAllSubsetNames(schema);
    if (!allSubsets.isEmpty()) {
      Map<String, Object> subsetsMap = new LinkedHashMap<>();
      for (String subset : allSubsets) {
        subsetsMap.put(subset, Map.of());
      }
      root.put(FIELD_SUBSETS, subsetsMap);

      Map<String, Object> templatesMap = new LinkedHashMap<>();
      Map<String, Object> allTemplate = new LinkedHashMap<>();
      allTemplate.put(FIELD_INCLUDES, List.copyOf(allSubsets));
      templatesMap.put("all", allTemplate);
      root.put(FIELD_TEMPLATES, templatesMap);
    }

    root.put(FIELD_IMPORTS, List.of(TABLES_DIR + "/"));

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Files.writeString(directory.resolve(MOLGENIS_YAML), mapper.writeValueAsString(root));

    for (TableMetadata table : schema.getTables()) {
      Map<String, Object> tableDoc = buildNewFormatTableDocumentFlat(table);
      String yaml = mapper.writeValueAsString(tableDoc);
      Files.writeString(tablesDir.resolve(table.getTableName() + ".yaml"), yaml);
    }
  }

  private static Set<String> collectAllSubsetNames(SchemaMetadata schema) {
    Set<String> subsets = new LinkedHashSet<>();
    for (TableMetadata table : schema.getTables()) {
      if (table.getSubsets() != null) {
        for (String subset : table.getSubsets()) {
          subsets.add(SubsetNameNormalizer.normalize(subset));
        }
      }
      for (Column column : table.getNonInheritedColumns()) {
        if (column.getSubsets() != null) {
          for (String subset : column.getSubsets()) {
            subsets.add(SubsetNameNormalizer.normalize(subset));
          }
        }
      }
    }
    return subsets;
  }

  static boolean isValidSubsetIdentifier(String id) {
    return SubsetNameNormalizer.isValidIdentifier(id);
  }

  private static List<String> normalizeSubsetIdentifiers(String[] subsets) {
    if (subsets == null || subsets.length == 0) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String subset : subsets) {
      result.add(SubsetNameNormalizer.normalize(subset));
    }
    return result;
  }

  public static void toBundleSingleFile(
      SchemaMetadata schema, String bundleName, String bundleDescription, Path outputFile)
      throws IOException {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_NAME, bundleName);
    if (bundleDescription != null) {
      doc.put(FIELD_DESCRIPTION, bundleDescription);
    }

    Map<String, Object> tablesMap = new LinkedHashMap<>();
    for (TableMetadata table : schema.getTables()) {
      if (isRootTable(table)) {
        tablesMap.put(table.getTableName(), buildNewFormatTableEntry(schema, table));
      }
    }
    doc.put(FIELD_TABLES, tablesMap);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Files.writeString(outputFile, mapper.writeValueAsString(doc));
  }

  private static Map<String, Object> buildNewFormatTableDocument(
      SchemaMetadata schema, TableMetadata table) {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_TABLE, table.getTableName());
    Map<String, Object> tableBody = buildNewFormatTableEntry(schema, table);
    doc.putAll(tableBody);
    return doc;
  }

  private static Map<String, Object> buildNewFormatTableDocumentFlat(TableMetadata table) {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_TABLE, table.getTableName());
    if (table.getDescription() != null) {
      doc.put(FIELD_DESCRIPTION, table.getDescription());
    }
    String[] inheritNames = table.getInheritNames();
    if (inheritNames != null && inheritNames.length > 0) {
      doc.put(FIELD_INHERITS, Arrays.asList(inheritNames));
    }
    if (TableType.INTERNAL.equals(table.getTableType())) {
      doc.put(FIELD_INTERNAL, true);
    }
    List<String> normalizedSubsets = normalizeSubsetIdentifiers(table.getSubsets());
    if (!normalizedSubsets.isEmpty()) {
      doc.put(FIELD_SUBSETS, normalizedSubsets);
    }
    if (table.getSemantics() != null && table.getSemantics().length > 0) {
      doc.put(FIELD_SEMANTICS, Arrays.asList(table.getSemantics()));
    }
    Map<String, Object> columnsMap = buildNewFormatColumnsMap(table.getNonInheritedColumns());
    doc.put(FIELD_COLUMNS, columnsMap);
    return doc;
  }

  private static Map<String, Object> buildNewFormatTableEntry(
      SchemaMetadata schema, TableMetadata table) {
    Map<String, Object> entry = new LinkedHashMap<>();

    if (table.getDescription() != null) {
      entry.put(FIELD_DESCRIPTION, table.getDescription());
    }
    List<String> tableSubsets = normalizeSubsetIdentifiers(table.getSubsets());
    if (!tableSubsets.isEmpty()) {
      entry.put(FIELD_SUBSETS, tableSubsets);
    }
    if (table.getSemantics() != null && table.getSemantics().length > 0) {
      entry.put(FIELD_SEMANTICS, Arrays.asList(table.getSemantics()));
    }

    List<TableMetadata> subtypes = findExtensionTables(schema, table.getTableName());
    if (!subtypes.isEmpty()) {
      Map<String, Object> subtypesMap = new LinkedHashMap<>();
      for (TableMetadata subtype : subtypes) {
        subtypesMap.put(subtype.getTableName(), buildSubtypeEntry(subtype, table.getTableName()));
      }
      entry.put(FIELD_SUBTYPES, subtypesMap);
    }

    Map<String, Object> columnsMap = buildNewFormatColumnsMap(table.getNonInheritedColumns());
    if (!columnsMap.isEmpty()) {
      entry.put(FIELD_COLUMNS, columnsMap);
    }

    return entry;
  }

  private static Map<String, Object> buildSubtypeEntry(
      TableMetadata subtype, String defaultParent) {
    Map<String, Object> entry = new LinkedHashMap<>();
    if (subtype.getDescription() != null) {
      entry.put(FIELD_DESCRIPTION, subtype.getDescription());
    }
    String[] inheritNames = subtype.getInheritNames();
    if (inheritNames != null
        && inheritNames.length > 0
        && !(inheritNames.length == 1 && defaultParent.equals(inheritNames[0]))) {
      entry.put(FIELD_INHERITS, Arrays.asList(inheritNames));
    }
    if (TableType.INTERNAL.equals(subtype.getTableType())) {
      entry.put(FIELD_INTERNAL, true);
    }
    List<String> subtypeSubsets = normalizeSubsetIdentifiers(subtype.getSubsets());
    if (!subtypeSubsets.isEmpty()) {
      entry.put(FIELD_SUBSETS, subtypeSubsets);
    }
    if (subtype.getSemantics() != null && subtype.getSemantics().length > 0) {
      entry.put(FIELD_SEMANTICS, Arrays.asList(subtype.getSemantics()));
    }
    return entry;
  }

  private static Map<String, Object> buildNewFormatColumnsMap(List<Column> columns) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Column col : columns) {
      if (col.isSystemColumn()) {
        continue;
      }
      result.put(col.getName(), buildNewFormatColumnEntry(col));
    }
    return result;
  }

  private static Map<String, Object> buildNewFormatColumnEntry(Column col) {
    Map<String, Object> entry = new LinkedHashMap<>();

    ColumnType colType = col.getColumnType();
    if (colType != null && !ColumnType.STRING.equals(colType)) {
      String typeStr = columnTypeToNewFormatString(colType);
      entry.put(FIELD_TYPE, typeStr);
    }
    if (col.getKey() > 0) {
      entry.put(FIELD_KEY, col.getKey());
    }
    if (col.getRequired() != null) {
      entry.put(FIELD_REQUIRED, col.getRequired());
    }
    if (col.getDefaultValue() != null) {
      entry.put(FIELD_DEFAULT_VALUE, col.getDefaultValue());
    }
    if (col.getRefTableName() != null) {
      entry.put(FIELD_REF_TABLE, col.getRefTableName());
    }
    if (col.getRefBack() != null) {
      entry.put(FIELD_REF_BACK, col.getRefBack());
    }
    if (col.getRefLink() != null) {
      entry.put(FIELD_REF_LINK, col.getRefLink());
    }
    if (col.getRefLabel() != null) {
      entry.put(FIELD_REF_LABEL, col.getRefLabel());
    }
    String colDescription = col.getDescriptions().get("en");
    if (colDescription != null) {
      entry.put(FIELD_DESCRIPTION, colDescription);
    }
    if (col.getSemantics() != null && col.getSemantics().length > 0) {
      entry.put(FIELD_SEMANTICS, Arrays.asList(col.getSemantics()));
    }
    List<String> colSubsets = normalizeSubsetIdentifiers(col.getSubsets());
    if (!colSubsets.isEmpty()) {
      entry.put(FIELD_SUBSETS, colSubsets);
    }
    if (col.getValidation() != null) {
      entry.put(FIELD_VALIDATION, col.getValidation());
    }
    if (col.getVisible() != null) {
      entry.put(FIELD_VISIBLE, col.getVisible());
    }
    if (col.getComputed() != null) {
      entry.put(FIELD_COMPUTED, col.getComputed());
    }
    if (Boolean.TRUE.equals(col.isReadonly())) {
      entry.put(FIELD_READONLY, true);
    }
    String colLabel = col.getLabel();
    if (colLabel != null && !colLabel.equals(col.getName())) {
      entry.put(FIELD_LABEL, colLabel);
    }
    return entry;
  }

  private static String columnTypeToNewFormatString(ColumnType colType) {
    if (ColumnType.EXTENSION.equals(colType)) {
      return TYPE_SUBTYPE;
    }
    if (ColumnType.EXTENSION_ARRAY.equals(colType)) {
      return TYPE_SUBTYPE_ARRAY;
    }
    return colType.toString().toLowerCase();
  }

  private static boolean isRootTable(TableMetadata table) {
    if (TableType.INTERNAL.equals(table.getTableType())) {
      return false;
    }
    String[] inheritNames = table.getInheritNames();
    return inheritNames == null || inheritNames.length == 0;
  }

  private static List<TableMetadata> findExtensionTables(
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
