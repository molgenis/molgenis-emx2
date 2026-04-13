package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
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
  private static final String FIELD_ACTIVE_PROFILES = "activeProfiles";
  private static final String FIELD_COLUMNS = "columns";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_EXTENDS = "extends";
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
  private static final String FIELD_VALUES = "values";
  private static final String FIELD_VARIANTS = "variants";
  private static final String FIELD_VARIANT = "variant";
  private static final String FIELD_TABLES = "tables";
  private static final String FIELD_NAMESPACES = "namespaces";
  private static final String RESERVED_COLUMNS = "columns";
  private static final String TYPE_VARIANT = "variant";
  private static final String TYPE_VARIANT_ARRAY = "variant_array";
  private static final String TYPE_EXTENSION = "extension";
  private static final String TYPE_EXTENSION_ARRAY = "extension_array";
  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String SINGLE_FILE_FORBIDDEN_ONTOLOGIES = "ontologies";
  private static final String SINGLE_FILE_FORBIDDEN_MIGRATIONS = "migrations";
  private static final String UNEXPANDED_IMPORT_MESSAGE = "Unexpanded 'import:'";

  private static final Logger log = LoggerFactory.getLogger(Emx2Yaml.class);

  private Emx2Yaml() {}

  private static ObjectMapper createYamlMapper() {
    YAMLFactory factory =
        YAMLFactory.builder()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            .disable(YAMLGenerator.Feature.SPLIT_LINES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();
    return new ObjectMapper(factory);
  }

  public static BundleResult fromBundle(Path bundlePathOrFile) throws IOException {
    if (Files.isRegularFile(bundlePathOrFile)) {
      ObjectMapper mapper = createYamlMapper();
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
    try {
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
    } catch (Exception e) {
      deleteDirectory(tempDir);
      if (e instanceof IOException ioEx) throw ioEx;
      throw new IOException("Failed to extract bundle from jar", e);
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
    ObjectMapper mapper = createYamlMapper();
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
        List.of(SINGLE_FILE_FORBIDDEN_ONTOLOGIES, SINGLE_FILE_FORBIDDEN_MIGRATIONS)) {
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
    Map<String, ProfileEntry> internalRegistry = toProfileEntryMap(bundle.profiles(), true);
    Map<String, ProfileEntry> profileRegistry = toProfileEntryMap(bundle.profiles(), false);
    Map<String, ProfileEntry> combinedRegistry = toCombinedEntryMap(bundle.profiles());
    SchemaMetadata schema = bundleToSchemaMetadata(bundle);

    BundleValidator.validate(name, combinedRegistry, schema);

    return new BundleResult(bundle, schema, internalRegistry, profileRegistry, Map.of());
  }

  @SuppressWarnings("unchecked")
  private static BundleResult fromBundleDirectory(Path directory) throws IOException {
    Path molgenisYaml = directory.resolve(MOLGENIS_YAML);
    if (!Files.exists(molgenisYaml)) {
      throw new MolgenisException("Directory bundle missing 'molgenis.yaml' at: " + directory);
    }

    ObjectMapper mapper = createYamlMapper();
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
    Map<String, ProfileEntry> internalRegistry = toProfileEntryMap(bundle.profiles(), true);
    Map<String, ProfileEntry> profileRegistry = toProfileEntryMap(bundle.profiles(), false);
    Map<String, ProfileEntry> combinedRegistry = toCombinedEntryMap(bundle.profiles());
    SchemaMetadata schema = bundleToSchemaMetadata(bundle);

    BundleValidator.validate(name, combinedRegistry, schema);

    return new BundleResult(bundle, schema, internalRegistry, profileRegistry, namespaces);
  }

  private static void loadTableFileIntoSchema(Path yamlFile, SchemaMetadata schema)
      throws IOException {
    ObjectMapper mapper = createYamlMapper();
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

  private static Map<String, ProfileEntry> toProfileEntryMap(
      List<ProfileDef> profileList, boolean internalOnly) {
    Map<String, ProfileEntry> result = new LinkedHashMap<>();
    for (ProfileDef def : profileList) {
      if (def.name() == null) {
        throw new MolgenisException("Profile entry missing required 'name:' field");
      }
      boolean isInternal = Boolean.TRUE.equals(def.internal());
      if (isInternal == internalOnly) {
        result.put(def.name(), new ProfileEntry(def.name(), def.description(), def.includes()));
      }
    }
    return result;
  }

  private static Map<String, ProfileEntry> toCombinedEntryMap(List<ProfileDef> profileList) {
    Map<String, ProfileEntry> result = new LinkedHashMap<>();
    for (ProfileDef def : profileList) {
      if (def.name() == null) {
        throw new MolgenisException("Profile entry missing required 'name:' field");
      }
      result.put(def.name(), new ProfileEntry(def.name(), def.description(), def.includes()));
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
    if (!tableDef.extendNames().isEmpty()) {
      rootTable.setExtendNames(tableDef.extendNames().toArray(new String[0]));
    }
    if (tableDef.isInternal()) {
      rootTable.setTableType(TableType.INTERNAL);
    }
    if (!tableDef.profiles().isEmpty()) {
      rootTable.setProfiles(tableDef.profiles().toArray(new String[0]));
    }

    Map<String, TableMetadata> variantTablesByName = new LinkedHashMap<>();
    for (VariantDef variantDef : tableDef.variants()) {
      if (variantDef.name() == null) {
        throw new MolgenisException(
            "Table '" + tableName + "': variant entry missing required 'name:' field");
      }
      String variantName = variantDef.name();
      TableMetadata variantTable = new TableMetadata(variantName);
      if (variantDef.extendNames().isEmpty()) {
        variantTable.setExtendNames(tableName);
      } else {
        variantTable.setExtendNames(variantDef.extendNames().toArray(new String[0]));
      }
      if (variantDef.isInternal()) {
        variantTable.setTableType(TableType.INTERNAL);
      }
      if (variantDef.description() != null) {
        variantTable.setDescription(variantDef.description());
      }
      if (!variantDef.profiles().isEmpty()) {
        variantTable.setProfiles(variantDef.profiles().toArray(new String[0]));
      }
      variantTablesByName.put(variantName, variantTable);
    }

    String[] tableInheritedProfiles =
        tableDef.profiles().isEmpty() ? null : tableDef.profiles().toArray(new String[0]);
    Set<String> seenColumnNames = new LinkedHashSet<>();
    parseColumnsAsList(
        tableDef.columns(),
        0,
        tableName,
        rootTable,
        variantTablesByName,
        null,
        tableInheritedProfiles,
        seenColumnNames);

    createIfAbsent(schema, rootTable);
    for (TableMetadata variantTable : variantTablesByName.values()) {
      createIfAbsent(schema, variantTable);
    }
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
    ObjectMapper mapper = createYamlMapper();
    Map<String, Object> normalized = new LinkedHashMap<>(tableMap);
    Object rawVariants = normalized.get(FIELD_VARIANTS);
    if (rawVariants instanceof Map) {
      Map<String, Object> variantsMap = (Map<String, Object>) rawVariants;
      List<Map<String, Object>> variantsList = new ArrayList<>();
      for (Map.Entry<String, Object> entry : variantsMap.entrySet()) {
        Map<String, Object> variantEntry = new LinkedHashMap<>();
        variantEntry.put(FIELD_NAME, entry.getKey());
        if (entry.getValue() instanceof Map) {
          variantEntry.putAll((Map<String, Object>) entry.getValue());
        }
        variantsList.add(variantEntry);
      }
      normalized.put(FIELD_VARIANTS, variantsList);
    }
    TableDef tableDef = mapper.convertValue(normalized, TableDef.class);
    materializeTableDef(tableName, tableDef, schema);
  }

  private static void createIfAbsent(SchemaMetadata schema, TableMetadata table) {
    if (schema.getTableMetadata(table.getTableName()) == null) {
      schema.create(table);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> columnsMapToList(Map<String, Object> columnsMap) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : columnsMap.entrySet()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put(FIELD_NAME, entry.getKey());
      if (entry.getValue() instanceof Map) {
        item.putAll((Map<String, Object>) entry.getValue());
      }
      result.add(item);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void parseColumnsAsList(
      List<Map<String, Object>> columnsList,
      int depth,
      String tableName,
      TableMetadata rootTable,
      Map<String, TableMetadata> variantTablesByName,
      String inheritedVariant,
      String[] inheritedProfiles,
      Set<String> seenColumnNames) {

    for (Map<String, Object> entry : columnsList) {
      if (entry.containsKey("import")) {
        throw new MolgenisException(
            UNEXPANDED_IMPORT_MESSAGE
                + " entry found in columns list for table '"
                + tableName
                + "'. Column imports must be expanded before parsing.");
      }

      if (!entry.containsKey("section")
          && !entry.containsKey("heading")
          && entry.containsKey(FIELD_VARIANT)
          && entry.containsKey(FIELD_COLUMNS)
          && !entry.containsKey(FIELD_NAME)) {
        String variantGroupName = (String) entry.get(FIELD_VARIANT);
        String[] groupProfiles;
        if (entry.containsKey(FIELD_PROFILES)) {
          groupProfiles = toStringArray((List<String>) entry.get(FIELD_PROFILES));
        } else {
          groupProfiles = inheritedProfiles;
        }
        Object nestedRaw = entry.get(FIELD_COLUMNS);
        List<Map<String, Object>> nestedList;
        if (nestedRaw instanceof List) {
          nestedList = (List<Map<String, Object>>) nestedRaw;
        } else if (nestedRaw instanceof Map) {
          nestedList = columnsMapToList((Map<String, Object>) nestedRaw);
        } else {
          nestedList = List.of();
        }
        parseColumnsAsList(
            nestedList,
            0,
            tableName,
            rootTable,
            variantTablesByName,
            variantGroupName,
            groupProfiles,
            new LinkedHashSet<>());
      } else if (entry.containsKey("section") || entry.containsKey("heading")) {
        String containerKey = entry.containsKey("section") ? "section" : "heading";
        String containerName = (String) entry.get(containerKey);

        if (depth >= 2) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "', entry '"
                  + containerName
                  + "': nesting depth exceeded (max 2: table → section → heading → columns)");
        }

        if (entry.containsKey(FIELD_SEMANTICS)) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "', "
                  + containerKey
                  + " '"
                  + containerName
                  + "': 'semantics:' is not allowed on a section or heading; set it per data column");
        }

        String containerVariant =
            entry.containsKey(FIELD_VARIANT) ? (String) entry.get(FIELD_VARIANT) : inheritedVariant;
        String[] containerProfiles;
        if (entry.containsKey(FIELD_PROFILES)) {
          containerProfiles =
              mergeProfiles(
                  inheritedProfiles, toStringArray((List<String>) entry.get(FIELD_PROFILES)));
        } else {
          containerProfiles = inheritedProfiles;
        }

        Column containerCol = new Column(containerName);
        containerCol.setType(
            "section".equals(containerKey) ? ColumnType.SECTION : ColumnType.HEADING);
        String containerDescription = (String) entry.get(FIELD_DESCRIPTION);
        if (containerDescription != null) {
          containerCol.setDescription(containerDescription);
        }
        if (containerProfiles != null && containerProfiles.length > 0) {
          containerCol.setProfiles(containerProfiles);
        }
        TableMetadata containerTarget =
            resolveTargetTable(
                containerVariant, rootTable, variantTablesByName, tableName, containerName);
        containerTarget.add(containerCol);

        Object nestedRaw = entry.get(FIELD_COLUMNS);
        List<Map<String, Object>> nestedList;
        if (nestedRaw instanceof List) {
          nestedList = (List<Map<String, Object>>) nestedRaw;
        } else if (nestedRaw instanceof Map) {
          nestedList = columnsMapToList((Map<String, Object>) nestedRaw);
        } else {
          nestedList = List.of();
        }

        parseColumnsAsList(
            nestedList,
            depth + 1,
            tableName,
            rootTable,
            variantTablesByName,
            containerVariant,
            containerProfiles,
            seenColumnNames);
      } else {
        String columnKey = (String) entry.get(FIELD_NAME);
        if (columnKey == null) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "': column entry missing 'name:' field (entry keys: "
                  + entry.keySet()
                  + ")");
        }

        if (RESERVED_COLUMNS.equals(columnKey)) {
          throw new MolgenisException(
              "Table '"
                  + tableName
                  + "': 'columns' is a reserved name and cannot be used as a column, section, or heading name");
        }

        Map<String, Object> columnAttrs = entry;
        boolean hasTypeHeading = "heading".equals(columnAttrs.get(FIELD_TYPE));

        if (hasTypeHeading) {
          Column headingCol = new Column(columnKey);
          headingCol.setType(ColumnType.HEADING);
          applyCommonColumnAttributes(headingCol, columnAttrs);
          String[] headingProfiles;
          if (columnAttrs.containsKey(FIELD_PROFILES)) {
            headingProfiles =
                mergeProfiles(
                    inheritedProfiles,
                    toStringArray((List<String>) columnAttrs.get(FIELD_PROFILES)));
          } else {
            headingProfiles = inheritedProfiles;
          }
          if (headingProfiles != null && headingProfiles.length > 0) {
            headingCol.setProfiles(headingProfiles);
          }
          TableMetadata target =
              resolveTargetTable(
                  inheritedVariant, rootTable, variantTablesByName, tableName, columnKey);
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

          String[] effectiveProfiles;
          if (columnAttrs.containsKey(FIELD_PROFILES)) {
            effectiveProfiles =
                mergeProfiles(
                    inheritedProfiles,
                    toStringArray((List<String>) columnAttrs.get(FIELD_PROFILES)));
          } else {
            effectiveProfiles = inheritedProfiles;
          }
          if (effectiveProfiles != null && effectiveProfiles.length > 0) {
            column.setProfiles(effectiveProfiles);
          }

          String effectiveVariant =
              columnAttrs.containsKey(FIELD_VARIANT)
                  ? (String) columnAttrs.get(FIELD_VARIANT)
                  : inheritedVariant;

          TableMetadata target =
              resolveTargetTable(
                  effectiveVariant, rootTable, variantTablesByName, tableName, columnKey);
          target.add(column);
        }
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
    if (TYPE_VARIANT.equals(normalized) || TYPE_EXTENSION.equals(normalized)) {
      return ColumnType.VARIANT;
    }
    if (TYPE_VARIANT_ARRAY.equals(normalized) || TYPE_EXTENSION_ARRAY.equals(normalized)) {
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

    Object defaultValue = attrs.get(FIELD_DEFAULT_VALUE);
    if (defaultValue != null) {
      column.setDefaultValue(defaultValue.toString());
    }

    Object refTable = attrs.get(FIELD_REF_TABLE);
    if (refTable != null) {
      column.setRefTable(refTable.toString());
    }

    Object refBack = attrs.get(FIELD_REF_BACK);
    if (refBack != null) {
      column.setRefBack(refBack.toString());
    }

    Object refLink = attrs.get(FIELD_REF_LINK);
    if (refLink != null) {
      column.setRefLink(refLink.toString());
    }

    Object refLabel = attrs.get(FIELD_REF_LABEL);
    if (refLabel != null) {
      column.setRefLabel(refLabel.toString());
    }

    Object refSchema = attrs.get(FIELD_REF_SCHEMA);
    if (refSchema != null) {
      column.setRefSchemaName(refSchema.toString());
    }

    Object computed = attrs.get(FIELD_COMPUTED);
    if (computed != null) {
      column.setComputed(computed.toString());
    }

    Object readonly = attrs.get(FIELD_READONLY);
    if (readonly != null) {
      if (readonly instanceof Boolean) {
        column.setReadonly((Boolean) readonly);
      } else {
        column.setReadonly(Boolean.parseBoolean(readonly.toString()));
      }
    }

    Object label = attrs.get(FIELD_LABEL);
    if (label != null) {
      column.setLabel(label.toString());
    }

    Object position = attrs.get(FIELD_POSITION);
    if (position != null) {
      column.setPosition(((Number) position).intValue());
    }

    Object oldName = attrs.get(FIELD_OLD_NAME);
    if (oldName != null) {
      column.setOldName(oldName.toString());
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

    Object values = attrs.get(FIELD_VALUES);
    if (values instanceof List) {
      column.setValues(((List<String>) values).toArray(new String[0]));
    } else if (values instanceof String) {
      column.setValues((String) values);
    }

    Object validation = attrs.get(FIELD_VALIDATION);
    if (validation != null) {
      column.setValidation(validation.toString());
    }

    Object visible = attrs.get(FIELD_VISIBLE);
    if (visible != null) {
      column.setVisible(visible.toString());
    }

    Object description = attrs.get(FIELD_DESCRIPTION);
    if (description != null) {
      column.setDescription(description.toString());
    }
  }

  private static String[] toStringArray(List<String> list) {
    if (list == null || list.isEmpty()) {
      return new String[0];
    }
    return list.toArray(new String[0]);
  }

  private static String[] mergeProfiles(String[] inherited, String[] declared) {
    if (inherited == null || inherited.length == 0) return declared;
    if (declared == null || declared.length == 0) return inherited;
    Set<String> merged = new LinkedHashSet<>(Arrays.asList(inherited));
    merged.addAll(Arrays.asList(declared));
    return merged.toArray(new String[0]);
  }

  public static class BundleResult {
    private final Bundle bundle;
    private final SchemaMetadata schema;
    private final Map<String, ProfileEntry> internalRegistry;
    private final Map<String, ProfileEntry> profileRegistry;
    private final Map<String, String> namespaces;

    public BundleResult(
        Bundle bundle,
        SchemaMetadata schema,
        Map<String, ProfileEntry> internalRegistry,
        Map<String, ProfileEntry> profileRegistry,
        Map<String, String> namespaces) {
      this.bundle = bundle;
      this.schema = schema;
      this.internalRegistry = internalRegistry;
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

    public Map<String, ProfileEntry> getInternalRegistry() {
      return internalRegistry;
    }

    public Map<String, ProfileEntry> getProfileRegistry() {
      return profileRegistry;
    }

    public Map<String, String> getNamespaces() {
      return namespaces;
    }

    public BundleContext toBundleContext() {
      return new BundleContext(
          bundle.name(), bundle.description(), schema, internalRegistry, profileRegistry);
    }
  }

  public static void toBundleDirectory(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      Path directory,
      List<ProfileDef> profileDefs)
      throws IOException {
    Path tablesDir = directory.resolve(TABLES_DIR);
    Files.createDirectories(tablesDir);

    Bundle bundle =
        SchemaMetadataToBundle.convertWithAutoRegistry(
            schema, bundleName, bundleDescription, profileDefs);

    ObjectMapper mapper = createYamlMapper();

    Map<String, Object> molgenisYamlDoc = buildMolgenisYamlDoc(bundle, TABLES_DIR + "/");
    Files.writeString(directory.resolve(MOLGENIS_YAML), mapper.writeValueAsString(molgenisYamlDoc));

    for (Map.Entry<String, TableDef> entry : bundle.tables().entrySet()) {
      Map<String, Object> tableDoc = buildTableDocument(entry.getKey(), entry.getValue());
      Files.writeString(
          tablesDir.resolve(entry.getKey() + ".yaml"), mapper.writeValueAsString(tableDoc));
    }
  }

  public static void toBundleSingleFile(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      Path outputFile,
      List<ProfileDef> profileDefs)
      throws IOException {
    Bundle bundle =
        SchemaMetadataToBundle.convertWithAutoRegistry(
            schema, bundleName, bundleDescription, profileDefs);
    Map<String, Object> doc = buildSingleFileDoc(bundle);
    ObjectMapper mapper = createYamlMapper();
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
      doc.put(FIELD_PROFILES, buildProfileDefList(bundle.profiles()));
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
      doc.put(FIELD_PROFILES, buildProfileDefList(bundle.profiles()));
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
    if (!table.extendNames().isEmpty()) {
      doc.put(FIELD_EXTENDS, table.extendNames());
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
      doc.put(FIELD_VARIANTS, buildVariantDefList(table.variants(), tableName));
    }
    if (!table.columns().isEmpty()) {
      doc.put(FIELD_COLUMNS, table.columns());
    }
    return doc;
  }

  private static Map<String, Object> buildTableEntry(TableDef table) {
    Map<String, Object> entry = new LinkedHashMap<>();
    putIfNotNull(entry, FIELD_DESCRIPTION, table.description());
    if (!table.extendNames().isEmpty()) {
      entry.put(FIELD_EXTENDS, table.extendNames());
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
      entry.put(FIELD_VARIANTS, buildVariantDefList(table.variants(), null));
    }
    if (!table.columns().isEmpty()) {
      entry.put(FIELD_COLUMNS, table.columns());
    }
    return entry;
  }

  private static List<Map<String, Object>> buildProfileDefList(List<ProfileDef> defList) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (ProfileDef def : defList) {
      Map<String, Object> defDoc = new LinkedHashMap<>();
      defDoc.put(FIELD_NAME, def.name());
      putIfNotNull(defDoc, FIELD_DESCRIPTION, def.description());
      if (!def.includes().isEmpty()) {
        defDoc.put("includes", def.includes());
      }
      if (Boolean.TRUE.equals(def.internal())) {
        defDoc.put(FIELD_INTERNAL, true);
      }
      result.add(defDoc);
    }
    return result;
  }

  private static List<Map<String, Object>> buildVariantDefList(
      List<VariantDef> variants, String defaultParent) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (VariantDef def : variants) {
      Map<String, Object> defDoc = new LinkedHashMap<>();
      defDoc.put(FIELD_NAME, def.name());
      putIfNotNull(defDoc, FIELD_DESCRIPTION, def.description());
      if (!def.extendNames().isEmpty()
          && !(def.extendNames().size() == 1 && def.extendNames().get(0).equals(defaultParent))) {
        defDoc.put(FIELD_EXTENDS, def.extendNames());
      }
      if (def.isInternal()) {
        defDoc.put(FIELD_INTERNAL, true);
      }
      if (!def.profiles().isEmpty()) {
        defDoc.put(FIELD_PROFILES, def.profiles());
      }
      result.add(defDoc);
    }
    return result;
  }

  private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  static boolean isValidProfileIdentifier(String id) {
    return ProfileNameNormalizer.isValidIdentifier(id);
  }

  @SuppressWarnings("unchecked")
  public static BundleParseResult fromYamlBundle(Path bundleFile) throws IOException {
    ObjectMapper mapper = createYamlMapper();
    Map<String, Object> yaml;
    try (InputStream inputStream = Files.newInputStream(bundleFile)) {
      yaml = mapper.readValue(inputStream, Map.class);
    }

    String name = (String) yaml.get(FIELD_NAME);
    if (name == null) {
      throw new MolgenisException("YAML bundle parse error: missing required 'name' field");
    }
    String description = (String) yaml.get(FIELD_DESCRIPTION);

    Path baseDir = bundleFile.getParent();
    SchemaMetadata schema = new SchemaMetadata();
    List<String> imports = (List<String>) yaml.getOrDefault(FIELD_IMPORTS, List.of());
    for (String importEntry : imports) {
      for (Path yamlFile : resolveImport(baseDir, importEntry)) {
        try {
          loadTableFileIntoSchema(yamlFile, schema);
        } catch (MolgenisException e) {
          if (e.getMessage() != null && e.getMessage().startsWith(UNEXPANDED_IMPORT_MESSAGE)) {
            log.warn(
                "Skipping table file '{}': contains unexpanded column imports not supported in bundle context",
                yamlFile.getFileName());
          } else {
            throw e;
          }
        }
      }
    }

    List<String> activeProfiles =
        (List<String>) yaml.getOrDefault(FIELD_ACTIVE_PROFILES, List.of());

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

    return new BundleParseResult(
        name, description, schema, activeProfiles, settings, permissions, fixedSchemas);
  }

  private static List<Path> resolveImport(Path baseDir, String importEntry) throws IOException {
    if (Path.of(importEntry).isAbsolute()) {
      throw new MolgenisException("Bundle import path must be relative: '" + importEntry + "'");
    }
    String trimmedEntry =
        importEntry.endsWith("/")
            ? importEntry.substring(0, importEntry.length() - 1)
            : importEntry;
    if (trimmedEntry.endsWith("/*")) {
      trimmedEntry = trimmedEntry.substring(0, trimmedEntry.length() - 2);
    }
    Path baseReal = baseDir.toRealPath();
    Path allowedRoot = baseReal.getParent() != null ? baseReal.getParent() : baseReal;
    Path resolved = baseReal.resolve(trimmedEntry).normalize();
    if (!resolved.startsWith(allowedRoot)) {
      throw new MolgenisException(
          "Bundle import path escapes base directory: '" + importEntry + "'");
    }
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
  public static String toYamlBundle(BundleParseResult result) throws IOException {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_NAME, result.getName());
    if (result.getDescription() != null) {
      doc.put(FIELD_DESCRIPTION, result.getDescription());
    }
    if (!result.getActiveProfiles().isEmpty()) {
      doc.put(FIELD_ACTIVE_PROFILES, result.getActiveProfiles());
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
    ObjectMapper mapper = createYamlMapper();
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

  public static class BundleParseResult {
    private final String name;
    private final String description;
    private final SchemaMetadata schema;
    private final List<String> activeProfiles;
    private final Map<String, String> settings;
    private final Map<String, String> permissions;
    private final List<FixedSchema> fixedSchemas;

    public BundleParseResult(
        String name,
        String description,
        SchemaMetadata schema,
        List<String> activeProfiles,
        Map<String, String> settings,
        Map<String, String> permissions,
        List<FixedSchema> fixedSchemas) {
      this.name = name;
      this.description = description;
      this.schema = schema;
      this.activeProfiles = activeProfiles;
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
      return activeProfiles;
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
}
