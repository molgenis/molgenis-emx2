package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
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

  private static final String FIELD_PROFILES = "profiles";
  private static final String FIELD_VARIANTS = "variants";
  private static final String FIELD_VARIANT = "variant";
  private static final String FIELD_TABLES = "tables";
  private static final String FIELD_NAMESPACES = "namespaces";
  private static final String RESERVED_COLUMNS = "columns";
  private static final String TYPE_VARIANT = "variant";
  private static final String TYPE_VARIANT_ARRAY = "variant_array";
  private static final String TYPE_EXTENSION = "extension";
  private static final String TYPE_EXTENSION_ARRAY = "extension_array";
  private static final String TYPE_SUBTYPE = "subtype";
  private static final String TYPE_SUBTYPE_ARRAY = "subtype_array";
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

  public static BundleResult fromBundleClasspath(String classpathDir) throws IOException {
    String normalised = classpathDir.endsWith("/") ? classpathDir : classpathDir + "/";
    String molgenisYamlPath = "/" + normalised + MOLGENIS_YAML;
    URL molgenisYamlUrl = Emx2Yaml.class.getResource(molgenisYamlPath);
    if (molgenisYamlUrl == null) {
      throw new MolgenisException("Bundle not found on classpath: " + molgenisYamlPath);
    }
    try {
      URI uri = molgenisYamlUrl.toURI().resolve(".");
      if ("jar".equals(uri.getScheme())) {
        Path tempDir = extractClasspathBundleToTemp(normalised, uri);
        try {
          return fromBundleDirectory(tempDir);
        } finally {
          deleteDirectory(tempDir);
        }
      } else {
        return fromBundleDirectory(Path.of(uri));
      }
    } catch (java.net.URISyntaxException e) {
      throw new IOException("Invalid classpath URL for bundle: " + molgenisYamlPath, e);
    }
  }

  private static Path extractClasspathBundleToTemp(String classpathDir, URI jarUri)
      throws IOException {
    Path tempDir = Files.createTempDirectory("molgenis_bundle_");
    String jarPath = jarUri.getSchemeSpecificPart();
    String jarFilePath = jarPath.substring(0, jarPath.indexOf('!'));
    String entryPrefix = jarPath.substring(jarPath.indexOf('!') + 2);
    URI fileUri = URI.create("jar:" + jarFilePath);
    try (FileSystem jarFs = FileSystems.newFileSystem(fileUri, Map.of())) {
      Path root = jarFs.getPath(entryPrefix);
      if (!Files.exists(root)) {
        throw new MolgenisException("Bundle directory not found in jar: " + entryPrefix);
      }
      try (var stream = Files.walk(root)) {
        for (Path source : (Iterable<Path>) stream::iterator) {
          Path relative = root.relativize(source);
          Path target = tempDir.resolve(relative.toString());
          if (Files.isDirectory(source)) {
            Files.createDirectories(target);
          } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }
    return tempDir;
  }

  private static void deleteDirectory(Path dir) throws IOException {
    try (var stream = Files.walk(dir)) {
      List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
      for (Path path : paths) {
        Files.deleteIfExists(path);
      }
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
    Map<String, ProfileEntry> subsetRegistry = toSubsetEntryMap(bundle.profiles(), true);
    Map<String, ProfileEntry> profileRegistry = toSubsetEntryMap(bundle.profiles(), false);
    Map<String, ProfileEntry> combinedRegistry = toCombinedEntryMap(bundle.profiles());
    SchemaMetadata schema = bundleToSchemaMetadata(bundle);

    BundleValidator.validate(name, combinedRegistry, schema);

    return new BundleResult(bundle, schema, subsetRegistry, profileRegistry, Map.of());
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
    Map<String, ProfileEntry> subsetRegistry = toSubsetEntryMap(bundle.profiles(), true);
    Map<String, ProfileEntry> profileRegistry = toSubsetEntryMap(bundle.profiles(), false);
    Map<String, ProfileEntry> combinedRegistry = toCombinedEntryMap(bundle.profiles());
    SchemaMetadata schema = bundleToSchemaMetadata(bundle);

    BundleValidator.validate(name, combinedRegistry, schema);

    return new BundleResult(bundle, schema, subsetRegistry, profileRegistry, namespaces);
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

  private static Map<String, ProfileEntry> toSubsetEntryMap(
      Map<String, ProfileDef> profileMap, boolean internalOnly) {
    Map<String, ProfileEntry> result = new LinkedHashMap<>();
    for (Map.Entry<String, ProfileDef> entry : profileMap.entrySet()) {
      boolean isInternal = Boolean.TRUE.equals(entry.getValue().internal());
      if (isInternal == internalOnly) {
        ProfileDef def = entry.getValue();
        result.put(
            entry.getKey(), new ProfileEntry(entry.getKey(), def.description(), def.includes()));
      }
    }
    return result;
  }

  private static Map<String, ProfileEntry> toCombinedEntryMap(Map<String, ProfileDef> profileMap) {
    Map<String, ProfileEntry> result = new LinkedHashMap<>();
    for (Map.Entry<String, ProfileDef> entry : profileMap.entrySet()) {
      ProfileDef def = entry.getValue();
      result.put(
          entry.getKey(), new ProfileEntry(entry.getKey(), def.description(), def.includes()));
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
    if (!tableDef.profiles().isEmpty()) {
      rootTable.setProfiles(tableDef.profiles().toArray(new String[0]));
    }

    Map<String, TableMetadata> variantTablesByName = new LinkedHashMap<>();
    for (Map.Entry<String, VariantDef> entry : tableDef.variants().entrySet()) {
      String variantName = entry.getKey();
      VariantDef variantDef = entry.getValue();
      TableMetadata variantTable = new TableMetadata(variantName);
      if (variantDef.inherits().isEmpty()) {
        variantTable.setInheritNames(tableName);
      } else {
        variantTable.setInheritNames(variantDef.inherits().toArray(new String[0]));
      }
      if (variantDef.isInternal()) {
        variantTable.setTableType(TableType.INTERNAL);
      }
      if (variantDef.description() != null) {
        variantTable.setDescription(variantDef.description());
      }
      variantTablesByName.put(variantName, variantTable);
    }

    Set<String> seenColumnNames = new LinkedHashSet<>();
    applyDataColumns(
        tableDef.columns(), tableName, rootTable, variantTablesByName, null, null, seenColumnNames);
    applySections(tableDef.sections(), tableName, rootTable, variantTablesByName, seenColumnNames);

    createIfAbsent(schema, rootTable);
    for (TableMetadata variantTable : variantTablesByName.values()) {
      createIfAbsent(schema, variantTable);
    }
  }

  private static void applyDataColumns(
      Map<String, DataColumn> columns,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> variantTablesByName,
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
          variantTablesByName,
          inheritedSubtype,
          inheritedSubsets,
          seenColumnNames);
    }
  }

  private static void applySections(
      Map<String, SectionDef> sections,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> variantTablesByName,
      Set<String> seenColumnNames) {

    for (Map.Entry<String, SectionDef> entry : sections.entrySet()) {
      SectionDef section = entry.getValue();
      String[] sectionSubsets =
          section.profiles() != null && !section.profiles().isEmpty()
              ? section.profiles().toArray(new String[0])
              : null;
      applyDataColumns(
          section.columns(),
          tableName,
          rootTable,
          variantTablesByName,
          section.variant(),
          sectionSubsets,
          seenColumnNames);
      applyHeadings(
          section.headings(),
          tableName,
          rootTable,
          variantTablesByName,
          section.variant(),
          sectionSubsets,
          seenColumnNames);
    }
  }

  private static void applyHeadings(
      Map<String, HeadingDef> headings,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> variantTablesByName,
      String inheritedSubtype,
      String[] inheritedSubsets,
      Set<String> seenColumnNames) {

    for (Map.Entry<String, HeadingDef> entry : headings.entrySet()) {
      HeadingDef heading = entry.getValue();
      String headingSubtype = heading.variant() != null ? heading.variant() : inheritedSubtype;
      String[] headingSubsets =
          heading.profiles() != null && !heading.profiles().isEmpty()
              ? heading.profiles().toArray(new String[0])
              : inheritedSubsets;
      applyDataColumns(
          heading.columns(),
          tableName,
          rootTable,
          variantTablesByName,
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
      Map<String, TableMetadata> variantTablesByName,
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
    if (col.profiles() != null) {
      effectiveSubsets = col.profiles().toArray(new String[0]);
    } else {
      effectiveSubsets = inheritedSubsets;
    }
    if (effectiveSubsets != null && effectiveSubsets.length > 0) {
      column.setProfiles(effectiveSubsets);
    }

    String effectiveSubtype = col.variant() != null ? col.variant() : inheritedSubtype;
    TableMetadata target =
        resolveTargetTable(effectiveSubtype, rootTable, variantTablesByName, tableName, columnName);
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

    Map<String, TableMetadata> variantTablesByName = new LinkedHashMap<>();
    Map<String, Object> rawVariants =
        (Map<String, Object>) tableMap.getOrDefault(FIELD_VARIANTS, Map.of());
    if (rawVariants.isEmpty()) {
      rawVariants = (Map<String, Object>) tableMap.getOrDefault("subtypes", Map.of());
    }
    for (Map.Entry<String, Object> variantEntry : rawVariants.entrySet()) {
      String variantName = variantEntry.getKey();
      Map<String, Object> variantMap =
          variantEntry.getValue() != null
              ? (Map<String, Object>) variantEntry.getValue()
              : Map.of();
      TableMetadata variantTable = new TableMetadata(variantName);
      List<String> inherits = (List<String>) variantMap.getOrDefault(FIELD_INHERITS, List.of());
      if (inherits.isEmpty()) {
        variantTable.setInheritNames(tableName);
      } else {
        variantTable.setInheritNames(inherits.toArray(new String[0]));
      }
      if (Boolean.TRUE.equals(variantMap.get(FIELD_INTERNAL))) {
        variantTable.setTableType(TableType.INTERNAL);
      }
      applyTableDescription(variantTable, variantMap);
      applyTableSubsets(variantTable, variantMap);
      variantTablesByName.put(variantName, variantTable);
    }

    Map<String, Object> columnsMap =
        (Map<String, Object>) tableMap.getOrDefault(FIELD_COLUMNS, Map.of());

    Set<String> seenColumnNames = new LinkedHashSet<>();
    parseColumnsAtDepth(
        columnsMap, 0, tableName, rootTable, variantTablesByName, null, null, seenColumnNames);

    createIfAbsent(schema, rootTable);
    for (TableMetadata variantTable : variantTablesByName.values()) {
      createIfAbsent(schema, variantTable);
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
      Map<String, TableMetadata> variantTablesByName,
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
            columnAttrs.containsKey(FIELD_VARIANT)
                ? (String) columnAttrs.get(FIELD_VARIANT)
                : columnAttrs.containsKey("subtype")
                    ? (String) columnAttrs.get("subtype")
                    : inheritedSubtype;
        String[] containerSubsets;
        if (columnAttrs.containsKey(FIELD_PROFILES)) {
          containerSubsets = toStringArray((List<String>) columnAttrs.get(FIELD_PROFILES));
        } else if (columnAttrs.containsKey("templates")) {
          containerSubsets = toStringArray((List<String>) columnAttrs.get("templates"));
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
            variantTablesByName,
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
          headingCol.setProfiles(inheritedSubsets);
        }
        TableMetadata target =
            resolveTargetTable(
                inheritedSubtype, rootTable, variantTablesByName, tableName, columnKey);
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
        if (columnAttrs.containsKey(FIELD_PROFILES)) {
          effectiveSubsets = toStringArray((List<String>) columnAttrs.get(FIELD_PROFILES));
        } else if (columnAttrs.containsKey("templates")) {
          effectiveSubsets = toStringArray((List<String>) columnAttrs.get("templates"));
        } else if (columnAttrs.containsKey("subsets")) {
          effectiveSubsets = toStringArray((List<String>) columnAttrs.get("subsets"));
        } else {
          effectiveSubsets = inheritedSubsets;
        }
        if (effectiveSubsets != null && effectiveSubsets.length > 0) {
          column.setProfiles(effectiveSubsets);
        }

        String effectiveSubtype =
            columnAttrs.containsKey(FIELD_VARIANT)
                ? (String) columnAttrs.get(FIELD_VARIANT)
                : columnAttrs.containsKey("subtype")
                    ? (String) columnAttrs.get("subtype")
                    : inheritedSubtype;

        TableMetadata target =
            resolveTargetTable(
                effectiveSubtype, rootTable, variantTablesByName, tableName, columnKey);
        target.add(column);
      }
    }
  }

  private static TableMetadata resolveTargetTable(
      String variantName,
      TableMetadata rootTable,
      Map<String, TableMetadata> variantTablesByName,
      String tableName,
      String columnKey) {
    if (variantName == null) {
      return rootTable;
    }
    TableMetadata target = variantTablesByName.get(variantName);
    if (target == null) {
      throw new MolgenisException(
          "Table '"
              + tableName
              + "', column '"
              + columnKey
              + "': references unknown variant '"
              + variantName
              + "'");
    }
    return target;
  }

  private static ColumnType resolveColumnType(String typeStr, String columnKey, String tableName) {
    String normalized = typeStr.toLowerCase().replace(" ", "_");
    if (TYPE_VARIANT.equals(normalized)
        || TYPE_SUBTYPE.equals(normalized)
        || TYPE_EXTENSION.equals(normalized)) {
      return ColumnType.VARIANT;
    }
    if (TYPE_VARIANT_ARRAY.equals(normalized)
        || TYPE_SUBTYPE_ARRAY.equals(normalized)
        || TYPE_EXTENSION_ARRAY.equals(normalized)) {
      return ColumnType.VARIANT_ARRAY;
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
    Object subsets = source.get(FIELD_PROFILES);
    if (subsets == null) {
      subsets = source.get("templates");
    }
    if (subsets == null) {
      subsets = source.get("subsets");
    }
    if (subsets instanceof List) {
      table.setProfiles(((List<String>) subsets).toArray(new String[0]));
    } else if (subsets instanceof String) {
      table.setProfiles((String) subsets);
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
    private final Bundle bundle;
    private final SchemaMetadata schema;
    private final Map<String, ProfileEntry> subsetRegistry;
    private final Map<String, ProfileEntry> profileRegistry;
    private final Map<String, String> namespaces;

    public BundleResult(
        Bundle bundle,
        SchemaMetadata schema,
        Map<String, ProfileEntry> subsetRegistry,
        Map<String, ProfileEntry> profileRegistry,
        Map<String, String> namespaces) {
      this.bundle = bundle;
      this.schema = schema;
      this.subsetRegistry = subsetRegistry;
      this.profileRegistry = profileRegistry;
      this.namespaces = namespaces;
    }

    public Bundle getBundle() {
      return bundle;
    }

    public String getName() {
      return bundle.name();
    }

    public String getDescription() {
      return bundle.description();
    }

    public SchemaMetadata getSchema() {
      return schema;
    }

    public Map<String, ProfileEntry> getProfileRegistry() {
      return subsetRegistry;
    }

    public Map<String, ProfileEntry> getTemplateRegistry() {
      return profileRegistry;
    }

    public Map<String, String> getNamespaces() {
      return namespaces;
    }

    public BundleContext toBundleContext() {
      return new BundleContext(
          bundle.name(), bundle.description(), schema, subsetRegistry, profileRegistry);
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
    if (!bundle.profiles().isEmpty()) {
      doc.put(FIELD_PROFILES, buildProfileDefMap(bundle.profiles()));
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
    if (!bundle.profiles().isEmpty()) {
      doc.put(FIELD_PROFILES, buildProfileDefMap(bundle.profiles()));
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
    if (!table.profiles().isEmpty()) {
      doc.put(FIELD_PROFILES, table.profiles());
    }
    if (!table.variants().isEmpty()) {
      doc.put(FIELD_VARIANTS, buildVariantDefMap(table.variants(), tableName));
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
    if (!table.profiles().isEmpty()) {
      entry.put(FIELD_PROFILES, table.profiles());
    }
    if (!table.variants().isEmpty()) {
      entry.put(FIELD_VARIANTS, buildVariantDefMap(table.variants(), null));
    }
    if (!table.columns().isEmpty()) {
      entry.put(FIELD_COLUMNS, buildDataColumnMap(table.columns()));
    }
    if (!table.sections().isEmpty()) {
      entry.put("sections", buildSectionDefMap(table.sections()));
    }
    return entry;
  }

  private static Map<String, Object> buildProfileDefMap(Map<String, ProfileDef> defMap) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, ProfileDef> entry : defMap.entrySet()) {
      ProfileDef def = entry.getValue();
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

  private static Map<String, Object> buildVariantDefMap(
      Map<String, VariantDef> variants, String defaultParent) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, VariantDef> entry : variants.entrySet()) {
      VariantDef def = entry.getValue();
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
    putIfNotNull(entry, FIELD_VARIANT, section.variant());
    if (!section.profiles().isEmpty()) {
      entry.put(FIELD_PROFILES, section.profiles());
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
    putIfNotNull(entry, FIELD_VARIANT, heading.variant());
    if (!heading.profiles().isEmpty()) {
      entry.put(FIELD_PROFILES, heading.profiles());
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
    if (col.profiles() != null && !col.profiles().isEmpty()) {
      entry.put(FIELD_PROFILES, col.profiles());
    }
    putIfNotNull(entry, FIELD_VALIDATION, col.validation());
    putIfNotNull(entry, FIELD_VISIBLE, col.visible());
    putIfNotNull(entry, FIELD_COMPUTED, col.computed());
    if (Boolean.TRUE.equals(col.readonly())) {
      entry.put(FIELD_READONLY, true);
    }
    putIfNotNull(entry, FIELD_LABEL, col.label());
    putIfNotNull(entry, FIELD_VARIANT, col.variant());
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
    return ProfileNameNormalizer.isValidIdentifier(id);
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
    if (!result.getActiveProfiles().isEmpty()) {
      doc.put(FIELD_ACTIVE_SUBSETS, result.getActiveProfiles());
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

    public List<String> getActiveProfiles() {
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
