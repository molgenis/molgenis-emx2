package org.molgenis.emx2.io.emx2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Reads and writes the YAML model bundle format at the {@link SchemaMetadata} level (no database).
 * This is the flat-root foundation: a {@code molgenis.yaml} referencing one file per table via
 * {@code tables: - file: ...}, plain columns with basic types, and bundle/table {@code settings:}.
 * Parsing is strict — any key outside the enumerated attribute surface fails with the source
 * position (see {@link YamlDocumentReader}). Later tickets extend the ALLOWED key sets and the
 * mapping in {@link #parseHierarchyFile} / {@link #columnToMap} for reuse, dotted refs and i18n;
 * the bundle envelope handling here stays unchanged.
 */
public class Emx2Yaml {

  public static final int SUPPORTED_FORMAT_VERSION = 1;

  static final String MOLGENIS_YAML = "molgenis.yaml";
  static final String TABLES_DIR = "tables/";

  private static final String KEY_FORMAT_VERSION = "formatVersion";
  private static final String KEY_VERSION = "version";
  private static final String KEY_TABLES = "tables";
  private static final String KEY_SETTINGS = "settings";
  private static final String KEY_FILE = "file";
  private static final String KEY_NAME = "name";
  private static final String KEY_LABEL = "label";
  private static final String KEY_DESCRIPTION = "description";
  private static final String KEY_COLUMNS = "columns";
  private static final String KEY_TYPE = "type";
  private static final String KEY_REF_TABLE = "refTable";
  private static final String KEY_REF_LINK = "refLink";
  private static final String KEY_REF_LABEL = "refLabel";
  private static final String KEY_REF_BACK = "refBack";
  private static final String KEY_KEY = "key";
  private static final String KEY_REQUIRED = "required";
  private static final String KEY_READONLY = "readonly";
  private static final String KEY_DEFAULT_VALUE = "defaultValue";
  private static final String KEY_VALIDATION = "validation";
  private static final String KEY_VISIBLE = "visible";
  private static final String KEY_COMPUTED = "computed";
  private static final String KEY_SEMANTICS = "semantics";
  private static final String KEY_VALUES = "values";
  private static final String KEY_FORM_LABEL = "formLabel";
  private static final String KEY_PROFILES = "profiles";
  private static final String KEY_PREVIOUS_NAMES = "previousNames";
  private static final String KEY_NAMESPACES = "namespaces";
  static final String KEY_SCHEMAS = "schemas";
  private static final String KEY_EXTENDS = "extends";
  private static final String KEY_TABLE_TYPE = "tableType";
  private static final String KEY_SUBCLASSES = "subclasses";
  private static final String KEY_MODULES = "modules";
  private static final String KEY_SUBCLASS = "subclass";
  private static final String KEY_MODULE = "module";
  private static final String KEY_IMPORTS = "imports";
  private static final String KEY_SECTION = "section";
  private static final String KEY_HEADING = "heading";

  private static final String DEFAULT_LOCALE = "en";
  private static final String LOCALE_SUFFIX = String.valueOf(YamlDocumentReader.LOCALE_SEPARATOR);
  private static final String BOOLEAN_TRUE = "true";
  private static final String BOOLEAN_FALSE = "false";
  private static final String SYSTEM_SETTING_PREFIX = "mg_";

  static final Set<String> BUNDLE_KEYS =
      Set.of(
          KEY_FORMAT_VERSION,
          KEY_VERSION,
          KEY_TABLES,
          KEY_SETTINGS,
          KEY_IMPORTS,
          KEY_NAMESPACES,
          KEY_SCHEMAS);
  static final Set<String> TABLE_KEYS =
      Set.of(
          KEY_NAME,
          KEY_LABEL,
          KEY_DESCRIPTION,
          KEY_SETTINGS,
          KEY_COLUMNS,
          KEY_EXTENDS,
          KEY_TABLE_TYPE,
          KEY_SUBCLASSES,
          KEY_MODULES,
          KEY_SEMANTICS,
          KEY_PROFILES,
          KEY_IMPORTS);
  static final Set<String> TABLE_ENTRY_KEYS = withKey(TABLE_KEYS, KEY_FILE);
  static final Set<String> SHARED_FILE_KEYS = Set.of(KEY_COLUMNS);
  static final Set<String> HEADING_KEYS =
      Set.of(
          KEY_SECTION,
          KEY_HEADING,
          KEY_VISIBLE,
          KEY_LABEL,
          KEY_DESCRIPTION,
          KEY_SUBCLASS,
          KEY_MODULE);
  static final Set<String> SUBTABLE_KEYS =
      Set.of(KEY_NAME, KEY_EXTENDS, KEY_LABEL, KEY_DESCRIPTION, KEY_TABLE_TYPE);
  static final Set<String> COLUMN_KEYS =
      Set.of(
          KEY_NAME,
          KEY_TYPE,
          KEY_REF_TABLE,
          KEY_REF_LINK,
          KEY_REF_LABEL,
          KEY_REF_BACK,
          KEY_KEY,
          KEY_REQUIRED,
          KEY_READONLY,
          KEY_DEFAULT_VALUE,
          KEY_VALIDATION,
          KEY_VISIBLE,
          KEY_COMPUTED,
          KEY_SEMANTICS,
          KEY_VALUES,
          KEY_PROFILES,
          KEY_PREVIOUS_NAMES,
          KEY_DESCRIPTION,
          KEY_LABEL,
          KEY_FORM_LABEL,
          KEY_SUBCLASS,
          KEY_MODULE);

  private Emx2Yaml() {}

  private static Set<String> withKey(Set<String> base, String extra) {
    Set<String> result = new HashSet<>(base);
    result.add(extra);
    return Set.copyOf(result);
  }

  public static Emx2YamlBundle fromBundle(Path bundlePathOrDir) throws IOException {
    Path directory =
        Files.isDirectory(bundlePathOrDir) ? bundlePathOrDir : bundlePathOrDir.getParent();
    Path molgenisYaml = directory.resolve(MOLGENIS_YAML);
    if (!Files.exists(molgenisYaml)) {
      throw new MolgenisException("Bundle is missing '" + MOLGENIS_YAML + "' at: " + directory);
    }
    Map<String, String> files = new LinkedHashMap<>();
    String rootContent = Files.readString(molgenisYaml);
    files.put(MOLGENIS_YAML, rootContent);
    readImports(rootContent, "bundle", BUNDLE_KEYS, directory, files);
    loadTableEntryFiles(rootContent, directory, files);
    return fromBundleFiles(files);
  }

  private static void loadTableEntryFiles(
      String rootContent, Path directory, Map<String, String> files) throws IOException {
    YamlDocumentReader reader = new YamlDocumentReader(MOLGENIS_YAML);
    Node rootNode = reader.compose(rootContent);
    if (rootNode == null) {
      return;
    }
    LinkedHashMap<String, Node> root = reader.mapping(rootNode, "bundle", BUNDLE_KEYS);
    Node tablesNode = root.get(KEY_TABLES);
    if (tablesNode == null) {
      return;
    }
    List<Node> entries = reader.sequence(tablesNode, KEY_TABLES);
    for (int index = 0; index < entries.size(); index++) {
      String path = KEY_TABLES + "[" + index + "]";
      LinkedHashMap<String, Node> entry =
          reader.mapping(entries.get(index), path, TABLE_ENTRY_KEYS);
      if (entry.containsKey(KEY_FILE)) {
        String relativePath = reader.scalar(entry.get(KEY_FILE), path + "." + KEY_FILE);
        if (!files.containsKey(relativePath)) {
          String tableContent = Files.readString(directory.resolve(relativePath));
          files.put(relativePath, tableContent);
          readImports(tableContent, "table", TABLE_KEYS, directory, files);
        }
      } else {
        loadImportFiles(reader, entry.get(KEY_IMPORTS), path + "." + KEY_IMPORTS, directory, files);
      }
    }
  }

  private static void loadImportFiles(
      YamlDocumentReader reader,
      Node importsNode,
      String path,
      Path directory,
      Map<String, String> files)
      throws IOException {
    if (importsNode == null) {
      return;
    }
    for (String importPath : reader.stringList(importsNode, path)) {
      if (!files.containsKey(importPath)) {
        files.put(importPath, Files.readString(directory.resolve(importPath)));
      }
    }
  }

  private static void readImports(
      String content,
      String label,
      Set<String> allowedKeys,
      Path directory,
      Map<String, String> files)
      throws IOException {
    for (String importPath : importPaths(content, label, allowedKeys)) {
      if (!files.containsKey(importPath)) {
        files.put(importPath, Files.readString(directory.resolve(importPath)));
      }
    }
  }

  private static List<String> importPaths(String content, String label, Set<String> allowedKeys) {
    YamlDocumentReader reader = new YamlDocumentReader(label);
    Node node = reader.compose(content);
    if (node == null) {
      return List.of();
    }
    LinkedHashMap<String, Node> map = reader.mapping(node, label, allowedKeys);
    Node importsNode = map.get(KEY_IMPORTS);
    if (importsNode == null) {
      return List.of();
    }
    return reader.stringList(importsNode, label + "." + KEY_IMPORTS);
  }

  public static Emx2YamlBundle fromBundleFiles(Map<String, String> files) {
    String rootContent = files.get(MOLGENIS_YAML);
    if (rootContent == null) {
      throw new MolgenisException("Bundle is missing '" + MOLGENIS_YAML + "'");
    }
    YamlDocumentReader reader = new YamlDocumentReader(MOLGENIS_YAML);
    Node rootNode = reader.compose(rootContent);
    if (rootNode == null) {
      throw new MolgenisException(MOLGENIS_YAML + " is empty");
    }
    LinkedHashMap<String, Node> root = reader.mapping(rootNode, "bundle", BUNDLE_KEYS);

    int formatVersion = readFormatVersion(reader, root);

    SchemaMetadata schema = new SchemaMetadata();
    Node settingsNode = root.get(KEY_SETTINGS);
    if (settingsNode != null) {
      schema.setSettings(reader.scalarMapping(settingsNode, "bundle." + KEY_SETTINGS));
    }
    Map<String, String> namespaces = Map.of();
    Node namespacesNode = root.get(KEY_NAMESPACES);
    if (namespacesNode != null) {
      namespaces = reader.scalarMapping(namespacesNode, "bundle." + KEY_NAMESPACES);
    }
    List<SharedFile> bundleImports =
        loadSharedFiles(reader, root.get(KEY_IMPORTS), files, "bundle." + KEY_IMPORTS);
    Map<String, Map<String, List<String>>> previousNames =
        parseTables(root.get(KEY_TABLES), files, schema, bundleImports);
    createImplicitOntologyTables(schema);

    String version =
        root.containsKey(KEY_VERSION) ? reader.scalar(root.get(KEY_VERSION), KEY_VERSION) : null;
    return new Emx2YamlBundle(schema, formatVersion, version, namespaces, previousNames);
  }

  private static int readFormatVersion(
      YamlDocumentReader reader, LinkedHashMap<String, Node> root) {
    if (!root.containsKey(KEY_FORMAT_VERSION)) {
      return SUPPORTED_FORMAT_VERSION;
    }
    int formatVersion = reader.integer(root.get(KEY_FORMAT_VERSION), KEY_FORMAT_VERSION);
    if (formatVersion > SUPPORTED_FORMAT_VERSION) {
      throw reader.error(
          "formatVersion "
              + formatVersion
              + " is newer than this build supports (up to "
              + SUPPORTED_FORMAT_VERSION
              + "); update MOLGENIS to read this bundle",
          root.get(KEY_FORMAT_VERSION));
    }
    return formatVersion;
  }

  private static void createImplicitOntologyTables(SchemaMetadata schema) {
    Set<String> implicitTables = new LinkedHashSet<>();
    for (TableMetadata table : schema.getTables()) {
      for (Column column : table.getColumns()) {
        if (column.isOntology()
            && column.getRefTableName() != null
            && Objects.equals(column.getRefSchemaName(), table.getSchemaName())
            && schema.getTableMetadata(column.getRefTableName()) == null) {
          implicitTables.add(column.getRefTableName());
        }
      }
    }
    for (String name : implicitTables) {
      schema.create(new TableMetadata(name).setTableType(TableType.ONTOLOGIES));
    }
  }

  private static Map<String, Map<String, List<String>>> parseTables(
      Node tablesNode,
      Map<String, String> files,
      SchemaMetadata schema,
      List<SharedFile> bundleImports) {
    Map<String, Map<String, List<String>>> previousNames = new LinkedHashMap<>();
    if (tablesNode == null) {
      return previousNames;
    }
    YamlDocumentReader reader = new YamlDocumentReader(MOLGENIS_YAML);
    List<Node> entries = reader.sequence(tablesNode, KEY_TABLES);
    List<ParsedFile> parsedFiles = new ArrayList<>();
    for (int index = 0; index < entries.size(); index++) {
      String path = KEY_TABLES + "[" + index + "]";
      Node entryNode = entries.get(index);
      LinkedHashMap<String, Node> entry = reader.mapping(entryNode, path, TABLE_ENTRY_KEYS);
      if (entry.containsKey(KEY_FILE)) {
        if (entry.size() > 1) {
          throw reader.error(
              "table entry cannot combine '"
                  + KEY_FILE
                  + "' with an inline table definition at '"
                  + path
                  + "'",
              entryNode);
        }
        String relativePath = reader.scalar(entry.get(KEY_FILE), path + "." + KEY_FILE);
        String content = files.get(relativePath);
        if (content == null) {
          throw reader.error(
              "referenced table file not found: " + relativePath, entry.get(KEY_FILE));
        }
        parsedFiles.add(parseHierarchyFile(relativePath, content, files, bundleImports));
      } else {
        LinkedHashMap<String, Node> tableMap = reader.mapping(entryNode, "table", TABLE_KEYS);
        parsedFiles.add(parseTable(reader, tableMap, entryNode, files, bundleImports));
      }
    }
    assemble(parsedFiles, schema, previousNames);
    return previousNames;
  }

  private static List<SharedFile> loadSharedFiles(
      YamlDocumentReader reader, Node importsNode, Map<String, String> files, String path) {
    if (importsNode == null) {
      return List.of();
    }
    List<SharedFile> sharedFiles = new ArrayList<>();
    for (String importPath : reader.stringList(importsNode, path)) {
      String content = files.get(importPath);
      if (content == null) {
        throw reader.error("imported file not found: " + importPath, importsNode);
      }
      sharedFiles.add(parseSharedFile(importPath, content));
    }
    return sharedFiles;
  }

  private static SharedFile parseSharedFile(String fileLabel, String content) {
    YamlDocumentReader reader = new YamlDocumentReader(fileLabel);
    Node fileNode = reader.compose(content);
    if (fileNode == null) {
      throw new MolgenisException(fileLabel + " is empty");
    }
    LinkedHashMap<String, Node> map = reader.mapping(fileNode, "shared", SHARED_FILE_KEYS);
    List<SharedEntry> entries = new ArrayList<>();
    Node columnsNode = map.get(KEY_COLUMNS);
    if (columnsNode != null) {
      List<Node> nodes = reader.sequence(columnsNode, "shared." + KEY_COLUMNS);
      for (int index = 0; index < nodes.size(); index++) {
        String path = fileLabel + " > " + KEY_COLUMNS + "[" + index + "]";
        Node node = nodes.get(index);
        if (reader.isScalar(node)) {
          throw reader.error(
              "shared file may only define columns and headings, not references", node);
        }
        Set<String> keys = reader.mappingKeys(node);
        if (keys.contains(KEY_SECTION) || keys.contains(KEY_HEADING)) {
          LinkedHashMap<String, Node> headingMap = reader.mapping(node, path, HEADING_KEYS);
          int level = keys.contains(KEY_SECTION) ? 0 : 1;
          Column column = parseHeadingColumn(reader, headingMap, path, level);
          entries.add(new SharedEntry(column.getName(), level, column));
        } else {
          LinkedHashMap<String, Node> columnMap = reader.mapping(node, path, COLUMN_KEYS);
          Column column = parseColumn(reader, columnMap, path);
          entries.add(new SharedEntry(column.getName(), null, column));
        }
      }
    }
    return new SharedFile(fileLabel, entries);
  }

  private static ParsedFile parseHierarchyFile(
      String fileLabel, String content, Map<String, String> files, List<SharedFile> bundleImports) {
    YamlDocumentReader reader = new YamlDocumentReader(fileLabel);
    Node fileNode = reader.compose(content);
    if (fileNode == null) {
      throw new MolgenisException(fileLabel + " is empty");
    }
    LinkedHashMap<String, Node> tableMap = reader.mapping(fileNode, "table", TABLE_KEYS);
    return parseTable(reader, tableMap, fileNode, files, bundleImports);
  }

  private static ParsedFile parseTable(
      YamlDocumentReader reader,
      LinkedHashMap<String, Node> tableMap,
      Node fileNode,
      Map<String, String> files,
      List<SharedFile> bundleImports) {
    Node nameNode = tableMap.get(KEY_NAME);
    if (nameNode == null) {
      throw reader.error("table file is missing '" + KEY_NAME + "'", fileNode);
    }
    String tableName = reader.scalar(nameNode, KEY_NAME);
    TableMetadata primary = new TableMetadata(tableName);

    applyLocalized(reader, tableMap, tableName, KEY_LABEL, primary::setLabel);
    applyLocalized(reader, tableMap, tableName, KEY_DESCRIPTION, primary::setDescription);
    if (tableMap.containsKey(KEY_SETTINGS)) {
      primary.setSettings(
          reader.scalarMapping(tableMap.get(KEY_SETTINGS), tableName + "." + KEY_SETTINGS));
    }
    if (tableMap.containsKey(KEY_EXTENDS)) {
      primary.setInheritNames(
          reader.stringList(tableMap.get(KEY_EXTENDS), tableName + "." + KEY_EXTENDS));
    }
    if (tableMap.containsKey(KEY_TABLE_TYPE)) {
      primary.setTableType(
          parseTableType(reader, tableMap.get(KEY_TABLE_TYPE), tableName + "." + KEY_TABLE_TYPE));
    }
    if (tableMap.containsKey(KEY_SEMANTICS)) {
      primary.setSemantics(
          reader
              .stringList(tableMap.get(KEY_SEMANTICS), tableName + "." + KEY_SEMANTICS)
              .toArray(new String[0]));
    }
    if (tableMap.containsKey(KEY_PROFILES)) {
      primary.setProfiles(
          reader
              .stringList(tableMap.get(KEY_PROFILES), tableName + "." + KEY_PROFILES)
              .toArray(new String[0]));
    }

    List<TableMetadata> subtables = new ArrayList<>();
    parseSubtables(reader, tableMap.get(KEY_SUBCLASSES), tableName, TableType.DATA, subtables);
    parseSubtables(reader, tableMap.get(KEY_MODULES), tableName, TableType.MODULE, subtables);

    List<SharedFile> tableImports =
        loadSharedFiles(reader, tableMap.get(KEY_IMPORTS), files, tableName + "." + KEY_IMPORTS);
    ImportScope scope = new ImportScope(tableImports, bundleImports);

    List<ColumnEntry> columns = parseColumns(reader, tableMap.get(KEY_COLUMNS), tableName, scope);
    return new ParsedFile(primary, subtables, columns);
  }

  private static void parseSubtables(
      YamlDocumentReader reader,
      Node blockNode,
      String rootName,
      TableType defaultType,
      List<TableMetadata> subtables) {
    if (blockNode == null) {
      return;
    }
    String blockName = TableType.MODULE.equals(defaultType) ? KEY_MODULES : KEY_SUBCLASSES;
    List<Node> entries = reader.sequence(blockNode, rootName + "." + blockName);
    for (int index = 0; index < entries.size(); index++) {
      String path = rootName + "." + blockName + "[" + index + "]";
      LinkedHashMap<String, Node> entry = reader.mapping(entries.get(index), path, SUBTABLE_KEYS);
      Node subNameNode = entry.get(KEY_NAME);
      if (subNameNode == null) {
        throw reader.error("subtable is missing '" + KEY_NAME + "'", entries.get(index));
      }
      TableMetadata subtable = new TableMetadata(reader.scalar(subNameNode, path + "." + KEY_NAME));
      if (entry.containsKey(KEY_EXTENDS)) {
        subtable.setInheritNames(
            reader.stringList(entry.get(KEY_EXTENDS), path + "." + KEY_EXTENDS));
      } else {
        subtable.setInheritNames(rootName);
      }
      if (entry.containsKey(KEY_TABLE_TYPE)) {
        subtable.setTableType(
            parseTableType(reader, entry.get(KEY_TABLE_TYPE), path + "." + KEY_TABLE_TYPE));
      } else {
        subtable.setTableType(defaultType);
      }
      applyLocalized(reader, entry, path, KEY_LABEL, subtable::setLabel);
      applyLocalized(reader, entry, path, KEY_DESCRIPTION, subtable::setDescription);
      subtables.add(subtable);
    }
  }

  private static List<ColumnEntry> parseColumns(
      YamlDocumentReader reader, Node columnsNode, String tableName, ImportScope scope) {
    List<ColumnEntry> result = new ArrayList<>();
    if (columnsNode == null) {
      return result;
    }
    List<Node> entries = reader.sequence(columnsNode, tableName + "." + KEY_COLUMNS);
    for (int index = 0; index < entries.size(); index++) {
      String path = tableName + " > " + KEY_COLUMNS + "[" + index + "]";
      Node node = entries.get(index);
      if (reader.isScalar(node)) {
        String referenceName = reader.scalar(node, path);
        for (Column spliced : scope.resolve(referenceName, reader, node)) {
          result.add(new ColumnEntry(tableName, spliced));
        }
      } else {
        result.add(parseDefinedEntry(reader, node, tableName, path, scope));
      }
    }
    rejectDuplicatePlacement(reader, result);
    return result;
  }

  private static ColumnEntry parseDefinedEntry(
      YamlDocumentReader reader, Node node, String tableName, String path, ImportScope scope) {
    Set<String> keys = reader.mappingKeys(node);
    if (keys.contains(KEY_SECTION) || keys.contains(KEY_HEADING)) {
      LinkedHashMap<String, Node> headingMap = reader.mapping(node, path, HEADING_KEYS);
      int level = keys.contains(KEY_SECTION) ? 0 : 1;
      Column column = parseHeadingColumn(reader, headingMap, path, level);
      rejectReuseOrDefine(reader, scope, column.getName(), node);
      String target = resolveMarkerTarget(reader, headingMap, tableName, path);
      return new ColumnEntry(target, column);
    }
    LinkedHashMap<String, Node> columnMap = reader.mapping(node, path, COLUMN_KEYS);
    String target = resolveMarkerTarget(reader, columnMap, tableName, path);
    Column column = parseColumn(reader, columnMap, path);
    rejectReuseOrDefine(reader, scope, column.getName(), node);
    List<String> previousNames =
        columnMap.containsKey(KEY_PREVIOUS_NAMES)
            ? reader.stringList(columnMap.get(KEY_PREVIOUS_NAMES), path + "." + KEY_PREVIOUS_NAMES)
            : List.of();
    return new ColumnEntry(target, column, previousNames);
  }

  private static void rejectReuseOrDefine(
      YamlDocumentReader reader, ImportScope scope, String name, Node node) {
    if (scope.defines(name)) {
      throw reader.error(
          "'"
              + name
              + "' is already defined by an imported shared file; reference it or choose a"
              + " different name (references cannot be refined)",
          node);
    }
  }

  private static void rejectDuplicatePlacement(
      YamlDocumentReader reader, List<ColumnEntry> result) {
    Map<String, Set<String>> placedPerTarget = new HashMap<>();
    for (ColumnEntry entry : result) {
      Set<String> placed =
          placedPerTarget.computeIfAbsent(entry.targetTable(), key -> new HashSet<>());
      if (!placed.add(entry.column().getName())) {
        throw new MolgenisException(
            reader.getFileLabel()
                + ": duplicate placement of '"
                + entry.column().getName()
                + "' in table '"
                + entry.targetTable()
                + "'");
      }
    }
  }

  private static Column parseHeadingColumn(
      YamlDocumentReader reader, LinkedHashMap<String, Node> headingMap, String path, int level) {
    String levelKey = level == 0 ? KEY_SECTION : KEY_HEADING;
    Column column = new Column(reader.scalar(headingMap.get(levelKey), path + "." + levelKey));
    column.setType(level == 0 ? ColumnType.SECTION : ColumnType.HEADING);
    if (headingMap.containsKey(KEY_VISIBLE)) {
      column.setVisible(reader.scalar(headingMap.get(KEY_VISIBLE), path + "." + KEY_VISIBLE));
    }
    applyLocalized(reader, headingMap, path, KEY_LABEL, column::setLabel);
    applyLocalized(reader, headingMap, path, KEY_DESCRIPTION, column::setDescription);
    return column;
  }

  private static String resolveMarkerTarget(
      YamlDocumentReader reader,
      LinkedHashMap<String, Node> columnMap,
      String tableName,
      String path) {
    Node subclassNode = columnMap.get(KEY_SUBCLASS);
    Node moduleNode = columnMap.get(KEY_MODULE);
    if (subclassNode != null && moduleNode != null) {
      throw reader.error(
          "column cannot carry both '"
              + KEY_SUBCLASS
              + "' and '"
              + KEY_MODULE
              + "' at '"
              + path
              + "'",
          columnMap.get(KEY_NAME));
    }
    if (subclassNode != null) {
      return reader.scalar(subclassNode, path + "." + KEY_SUBCLASS);
    }
    if (moduleNode != null) {
      return reader.scalar(moduleNode, path + "." + KEY_MODULE);
    }
    return tableName;
  }

  private static Column parseColumn(
      YamlDocumentReader reader, LinkedHashMap<String, Node> columnMap, String path) {
    Node nameNode = columnMap.get(KEY_NAME);
    if (nameNode == null) {
      throw new MolgenisException(reader.getFileLabel() + ": column is missing '" + KEY_NAME + "'");
    }
    Column column = new Column(reader.scalar(nameNode, path + "." + KEY_NAME));

    if (columnMap.containsKey(KEY_TYPE)) {
      column.setType(parseType(reader, columnMap.get(KEY_TYPE), path));
    }
    if (columnMap.containsKey(KEY_REF_TABLE)) {
      applyRefTable(
          column, reader.scalar(columnMap.get(KEY_REF_TABLE), path + "." + KEY_REF_TABLE));
    }
    if (columnMap.containsKey(KEY_REF_LINK)) {
      column.setRefLink(reader.scalar(columnMap.get(KEY_REF_LINK), path + "." + KEY_REF_LINK));
    }
    if (columnMap.containsKey(KEY_REF_BACK)) {
      column.setRefBack(reader.scalar(columnMap.get(KEY_REF_BACK), path + "." + KEY_REF_BACK));
    }
    if (columnMap.containsKey(KEY_REF_LABEL)) {
      column.setRefLabel(reader.scalar(columnMap.get(KEY_REF_LABEL), path + "." + KEY_REF_LABEL));
    }
    if (columnMap.containsKey(KEY_PROFILES)) {
      column.setProfiles(
          reader
              .stringList(columnMap.get(KEY_PROFILES), path + "." + KEY_PROFILES)
              .toArray(new String[0]));
    }
    if (columnMap.containsKey(KEY_KEY)) {
      column.setKey(reader.integer(columnMap.get(KEY_KEY), path + "." + KEY_KEY));
    }
    if (columnMap.containsKey(KEY_REQUIRED)) {
      column.setRequired(reader.scalar(columnMap.get(KEY_REQUIRED), path + "." + KEY_REQUIRED));
    }
    if (columnMap.containsKey(KEY_READONLY)) {
      column.setReadonly(
          BOOLEAN_TRUE.equals(
              reader.scalar(columnMap.get(KEY_READONLY), path + "." + KEY_READONLY)));
    }
    if (columnMap.containsKey(KEY_DEFAULT_VALUE)) {
      column.setDefaultValue(
          reader.scalar(columnMap.get(KEY_DEFAULT_VALUE), path + "." + KEY_DEFAULT_VALUE));
    }
    if (columnMap.containsKey(KEY_VALIDATION)) {
      column.setValidation(
          reader.scalar(columnMap.get(KEY_VALIDATION), path + "." + KEY_VALIDATION));
    }
    if (columnMap.containsKey(KEY_VISIBLE)) {
      column.setVisible(reader.scalar(columnMap.get(KEY_VISIBLE), path + "." + KEY_VISIBLE));
    }
    if (columnMap.containsKey(KEY_COMPUTED)) {
      column.setComputed(reader.scalar(columnMap.get(KEY_COMPUTED), path + "." + KEY_COMPUTED));
    }
    if (columnMap.containsKey(KEY_SEMANTICS)) {
      column.setSemantics(
          reader
              .stringList(columnMap.get(KEY_SEMANTICS), path + "." + KEY_SEMANTICS)
              .toArray(new String[0]));
    }
    if (columnMap.containsKey(KEY_VALUES)) {
      column.setValues(reader.stringList(columnMap.get(KEY_VALUES), path + "." + KEY_VALUES));
    }
    applyLocalized(reader, columnMap, path, KEY_DESCRIPTION, column::setDescription);
    applyLocalized(reader, columnMap, path, KEY_LABEL, column::setLabel);
    if (columnMap.containsKey(KEY_FORM_LABEL)) {
      column.setFormLabel(
          reader.scalar(columnMap.get(KEY_FORM_LABEL), path + "." + KEY_FORM_LABEL));
    }
    return column;
  }

  private static void applyRefTable(Column column, String raw) {
    int dot = raw.indexOf('.');
    if (dot > 0) {
      column.setRefSchemaName(raw.substring(0, dot));
      column.setRefTable(raw.substring(dot + 1));
    } else {
      column.setRefTable(raw);
    }
  }

  private static void applyLocalized(
      YamlDocumentReader reader,
      LinkedHashMap<String, Node> map,
      String path,
      String baseKey,
      BiConsumer<String, String> setter) {
    String localePrefix = baseKey + LOCALE_SUFFIX;
    for (Map.Entry<String, Node> entry : map.entrySet()) {
      String key = entry.getKey();
      if (key.equals(baseKey)) {
        setter.accept(reader.scalar(entry.getValue(), path + "." + key), DEFAULT_LOCALE);
      } else if (key.startsWith(localePrefix)) {
        setter.accept(
            reader.scalar(entry.getValue(), path + "." + key),
            key.substring(localePrefix.length()));
      }
    }
  }

  private static ColumnType parseType(YamlDocumentReader reader, Node typeNode, String path) {
    String raw = reader.scalar(typeNode, path + "." + KEY_TYPE);
    try {
      return ColumnType.valueOf(raw.toUpperCase().trim());
    } catch (IllegalArgumentException exception) {
      throw reader.error(
          "unknown column type '" + raw + "' at '" + path + "." + KEY_TYPE + "'", typeNode);
    }
  }

  private static TableType parseTableType(YamlDocumentReader reader, Node typeNode, String path) {
    String raw = reader.scalar(typeNode, path);
    try {
      return TableType.valueOf(raw.toUpperCase().trim());
    } catch (IllegalArgumentException exception) {
      throw reader.error("unknown tableType '" + raw + "' at '" + path + "'", typeNode);
    }
  }

  private static void assemble(
      List<ParsedFile> parsedFiles,
      SchemaMetadata schema,
      Map<String, Map<String, List<String>>> previousNames) {
    Map<String, Integer> fileOfTable = new HashMap<>();
    for (int index = 0; index < parsedFiles.size(); index++) {
      ParsedFile parsedFile = parsedFiles.get(index);
      fileOfTable.put(parsedFile.primary().getTableName(), index);
      for (TableMetadata subtable : parsedFile.subtables()) {
        fileOfTable.put(subtable.getTableName(), index);
      }
    }

    List<Integer> order = orderFiles(parsedFiles, fileOfTable);

    for (int fileIndex : order) {
      ParsedFile parsedFile = parsedFiles.get(fileIndex);
      schema.create(parsedFile.primary());
      for (TableMetadata subtable : parsedFile.subtables()) {
        schema.create(subtable);
      }
    }

    int position = 0;
    for (int fileIndex : order) {
      for (ColumnEntry entry : parsedFiles.get(fileIndex).columns()) {
        TableMetadata target = schema.getTableMetadata(entry.targetTable());
        if (target == null) {
          throw new MolgenisException(
              "column '"
                  + entry.column().getName()
                  + "' is assigned to unknown subtable '"
                  + entry.targetTable()
                  + "'");
        }
        entry.column().setPosition(position++);
        target.add(entry.column());
        if (!entry.previousNames().isEmpty()) {
          previousNames
              .computeIfAbsent(entry.targetTable(), key -> new LinkedHashMap<>())
              .put(entry.column().getName(), entry.previousNames());
        }
      }
    }
  }

  private static List<Integer> orderFiles(
      List<ParsedFile> parsedFiles, Map<String, Integer> fileOfTable) {
    int count = parsedFiles.size();
    List<Set<Integer>> adjacency = new ArrayList<>();
    int[] indegree = new int[count];
    for (int index = 0; index < count; index++) {
      adjacency.add(new LinkedHashSet<>());
    }

    for (int index = 0; index < count; index++) {
      ParsedFile parsedFile = parsedFiles.get(index);
      List<TableMetadata> tables = new ArrayList<>();
      tables.add(parsedFile.primary());
      tables.addAll(parsedFile.subtables());
      for (TableMetadata table : tables) {
        addInheritanceEdges(index, table.getInheritNames(), fileOfTable, adjacency, indegree);
      }
    }

    PriorityQueue<Integer> ready = new PriorityQueue<>();
    for (int index = 0; index < count; index++) {
      if (indegree[index] == 0) {
        ready.add(index);
      }
    }
    List<Integer> order = new ArrayList<>();
    while (!ready.isEmpty()) {
      int current = ready.poll();
      order.add(current);
      for (int next : adjacency.get(current)) {
        if (--indegree[next] == 0) {
          ready.add(next);
        }
      }
    }
    if (order.size() != count) {
      throw new MolgenisException("cyclic dependency between hierarchy files");
    }
    return order;
  }

  private static void addInheritanceEdges(
      int childFile,
      List<String> parents,
      Map<String, Integer> fileOfTable,
      List<Set<Integer>> adjacency,
      int[] indegree) {
    for (int parentIndex = 0; parentIndex < parents.size(); parentIndex++) {
      Integer parentFile = fileOfTable.get(parents.get(parentIndex));
      if (parentFile != null && parentFile != childFile) {
        addEdge(parentFile, childFile, adjacency, indegree);
      }
      if (parentIndex > 0) {
        Integer previousFile = fileOfTable.get(parents.get(parentIndex - 1));
        if (previousFile != null && parentFile != null && !previousFile.equals(parentFile)) {
          addEdge(previousFile, parentFile, adjacency, indegree);
        }
      }
    }
  }

  private static void addEdge(int from, int to, List<Set<Integer>> adjacency, int[] indegree) {
    if (adjacency.get(from).add(to)) {
      indegree[to]++;
    }
  }

  private record ParsedFile(
      TableMetadata primary, List<TableMetadata> subtables, List<ColumnEntry> columns) {}

  private record ColumnEntry(String targetTable, Column column, List<String> previousNames) {
    ColumnEntry(String targetTable, Column column) {
      this(targetTable, column, List.of());
    }
  }

  private record ColumnExport(String markerKey, String markerValue, Column column) {}

  private record SharedEntry(String name, Integer level, Column column) {}

  private record SharedFile(String fileLabel, List<SharedEntry> entries) {
    List<Integer> indicesOf(String name) {
      List<Integer> indices = new ArrayList<>();
      for (int index = 0; index < entries.size(); index++) {
        if (entries.get(index).name().equals(name)) {
          indices.add(index);
        }
      }
      return indices;
    }

    List<Column> spliceAt(int index) {
      SharedEntry entry = entries.get(index);
      List<Column> spliced = new ArrayList<>();
      spliced.add(new Column(entry.column()));
      if (entry.level() != null) {
        for (int next = index + 1; next < entries.size(); next++) {
          SharedEntry following = entries.get(next);
          if (following.level() != null && following.level() <= entry.level()) {
            break;
          }
          spliced.add(new Column(following.column()));
        }
      }
      return spliced;
    }
  }

  private record ImportScope(List<SharedFile> tableImports, List<SharedFile> bundleImports) {

    boolean defines(String name) {
      return matchCount(tableImports, name) > 0 || matchCount(bundleImports, name) > 0;
    }

    List<Column> resolve(String name, YamlDocumentReader reader, Node node) {
      List<Column> tableResolved = resolveIn(tableImports, name, reader, node);
      if (tableResolved != null) {
        return tableResolved;
      }
      List<Column> bundleResolved = resolveIn(bundleImports, name, reader, node);
      if (bundleResolved != null) {
        return bundleResolved;
      }
      throw reader.error("cannot resolve reference '" + name + "'", node);
    }

    private static int matchCount(List<SharedFile> scope, String name) {
      int count = 0;
      for (SharedFile sharedFile : scope) {
        count += sharedFile.indicesOf(name).size();
      }
      return count;
    }

    private static List<Column> resolveIn(
        List<SharedFile> scope, String name, YamlDocumentReader reader, Node node) {
      SharedFile matchedFile = null;
      int matchedIndex = -1;
      int matches = 0;
      for (SharedFile sharedFile : scope) {
        for (int index : sharedFile.indicesOf(name)) {
          matches++;
          matchedFile = sharedFile;
          matchedIndex = index;
        }
      }
      if (matches == 0) {
        return null;
      }
      if (matches > 1) {
        throw reader.error(
            "reference '" + name + "' is ambiguous: defined by more than one imported file", node);
      }
      return matchedFile.spliceAt(matchedIndex);
    }
  }

  public static Map<String, String> toBundleFiles(Emx2YamlBundle bundle) {
    Map<String, String> files = new LinkedHashMap<>();
    SchemaMetadata schema = bundle.schema();

    Map<String, Object> root = new LinkedHashMap<>();
    root.put(KEY_FORMAT_VERSION, bundle.formatVersion());
    if (bundle.version() != null) {
      root.put(KEY_VERSION, bundle.version());
    }
    if (!bundle.namespaces().isEmpty()) {
      root.put(KEY_NAMESPACES, new LinkedHashMap<>(bundle.namespaces()));
    }

    List<Map<String, Object>> tableEntries = new ArrayList<>();
    List<TableMetadata> rootTables = schema.getRootTables();
    for (TableMetadata table : rootTables) {
      if (TableType.ONTOLOGIES.equals(table.getTableType())) {
        continue;
      }
      String relativePath = TABLES_DIR + table.getTableName() + ".yaml";
      tableEntries.add(new LinkedHashMap<>(Map.of(KEY_FILE, relativePath)));
      files.put(relativePath, dump(tableToMap(table, schema, bundle.previousNames())));
    }
    root.put(KEY_TABLES, tableEntries);

    if (!schema.getSettings().isEmpty()) {
      root.put(KEY_SETTINGS, new LinkedHashMap<>(schema.getSettings()));
    }

    files.put(MOLGENIS_YAML, dump(root));
    return files;
  }

  public static String toSingleFile(Emx2YamlBundle bundle) {
    SchemaMetadata schema = bundle.schema();
    Map<String, Object> root = new LinkedHashMap<>();
    root.put(KEY_FORMAT_VERSION, bundle.formatVersion());
    if (bundle.version() != null) {
      root.put(KEY_VERSION, bundle.version());
    }
    if (!bundle.namespaces().isEmpty()) {
      root.put(KEY_NAMESPACES, new LinkedHashMap<>(bundle.namespaces()));
    }
    List<Map<String, Object>> tableEntries = new ArrayList<>();
    for (TableMetadata table : schema.getRootTables()) {
      if (TableType.ONTOLOGIES.equals(table.getTableType())) {
        continue;
      }
      tableEntries.add(tableToMap(table, schema, bundle.previousNames()));
    }
    root.put(KEY_TABLES, tableEntries);
    Map<String, String> exportableSettings = exportableSettings(schema.getSettings());
    if (!exportableSettings.isEmpty()) {
      root.put(KEY_SETTINGS, new LinkedHashMap<>(exportableSettings));
    }
    return dump(root);
  }

  private static Map<String, String> exportableSettings(Map<String, String> settings) {
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : settings.entrySet()) {
      if (!entry.getKey().startsWith(SYSTEM_SETTING_PREFIX)) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private static Map<String, Object> tableToMap(
      TableMetadata table,
      SchemaMetadata schema,
      Map<String, Map<String, List<String>>> previousNames) {
    Map<String, Object> tableMap = new LinkedHashMap<>();
    tableMap.put(KEY_NAME, table.getTableName());
    putLocalized(tableMap, KEY_LABEL, table.getLabels());
    putLocalized(tableMap, KEY_DESCRIPTION, table.getDescriptions());
    if (!table.getSettings().isEmpty()) {
      tableMap.put(KEY_SETTINGS, new LinkedHashMap<>(table.getSettings()));
    }
    if (table.getSemantics() != null && table.getSemantics().length > 0) {
      tableMap.put(KEY_SEMANTICS, List.of(table.getSemantics()));
    }
    if (table.getProfiles() != null && table.getProfiles().length > 0) {
      tableMap.put(KEY_PROFILES, List.of(table.getProfiles()));
    }

    List<TableMetadata> subclasses = new ArrayList<>();
    List<TableMetadata> modules = new ArrayList<>();
    collectDescendants(table, schema, subclasses, modules);
    if (!subclasses.isEmpty()) {
      tableMap.put(KEY_SUBCLASSES, subtableBlock(subclasses, table.getTableName(), TableType.DATA));
    }
    if (!modules.isEmpty()) {
      tableMap.put(KEY_MODULES, subtableBlock(modules, table.getTableName(), TableType.MODULE));
    }

    List<Map<String, Object>> columns = wovenColumns(table, subclasses, modules, previousNames);
    if (!columns.isEmpty()) {
      tableMap.put(KEY_COLUMNS, columns);
    }
    return tableMap;
  }

  private static void collectDescendants(
      TableMetadata root,
      SchemaMetadata schema,
      List<TableMetadata> subclasses,
      List<TableMetadata> modules) {
    for (TableMetadata candidate : schema.getTables()) {
      if (candidate.getTableName().equals(root.getTableName())
          || candidate.getInheritNames().isEmpty()
          || !root.getTableName().equals(candidate.getRootTable().getTableName())) {
        continue;
      }
      if (candidate.getTableType().isModule()) {
        modules.add(candidate);
      } else {
        subclasses.add(candidate);
      }
    }
    subclasses.sort(Comparator.comparing(TableMetadata::getTableName));
    modules.sort(Comparator.comparing(TableMetadata::getTableName));
  }

  private static List<Map<String, Object>> subtableBlock(
      List<TableMetadata> subtables, String rootName, TableType defaultType) {
    List<Map<String, Object>> block = new ArrayList<>();
    for (TableMetadata subtable : subtables) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put(KEY_NAME, subtable.getTableName());
      List<String> parents = subtable.getInheritNames();
      if (!(parents.size() == 1 && parents.get(0).equals(rootName))) {
        entry.put(KEY_EXTENDS, new ArrayList<>(parents));
      }
      if (!defaultType.equals(subtable.getTableType())) {
        entry.put(KEY_TABLE_TYPE, subtable.getTableType().toString().toLowerCase());
      }
      putLocalized(entry, KEY_LABEL, subtable.getLabels());
      putLocalized(entry, KEY_DESCRIPTION, subtable.getDescriptions());
      block.add(entry);
    }
    return block;
  }

  private static List<Map<String, Object>> wovenColumns(
      TableMetadata root,
      List<TableMetadata> subclasses,
      List<TableMetadata> modules,
      Map<String, Map<String, List<String>>> previousNames) {
    List<ColumnExport> exports = new ArrayList<>();
    for (Column column : ownColumns(root)) {
      exports.add(new ColumnExport(null, null, column));
    }
    for (TableMetadata subclass : subclasses) {
      for (Column column : ownColumns(subclass)) {
        exports.add(new ColumnExport(KEY_SUBCLASS, subclass.getTableName(), column));
      }
    }
    for (TableMetadata module : modules) {
      for (Column column : ownColumns(module)) {
        exports.add(new ColumnExport(KEY_MODULE, module.getTableName(), column));
      }
    }
    exports.sort(
        Comparator.comparingInt(
            export ->
                export.column().getPosition() != null
                    ? export.column().getPosition()
                    : Integer.MAX_VALUE));
    List<Map<String, Object>> columns = new ArrayList<>();
    for (ColumnExport export : exports) {
      columns.add(
          columnToMap(export.column(), export.markerKey(), export.markerValue(), previousNames));
    }
    return columns;
  }

  private static List<Column> ownColumns(TableMetadata table) {
    return table.getNonInheritedColumns().stream()
        .filter(column -> !column.isSystemColumn())
        .toList();
  }

  private static Map<String, Object> columnToMap(
      Column column,
      String markerKey,
      String markerValue,
      Map<String, Map<String, List<String>>> previousNames) {
    if (column.isHeading()) {
      return headingToMap(column, markerKey, markerValue);
    }
    Map<String, Object> columnMap = new LinkedHashMap<>();
    columnMap.put(KEY_NAME, column.getName());
    if (markerKey != null) {
      columnMap.put(markerKey, markerValue);
    }
    ColumnType type = column.getColumnType();
    if (type != null && !ColumnType.STRING.equals(type)) {
      columnMap.put(KEY_TYPE, type.toString().toLowerCase());
    }
    String refTableExport = refTableExport(column);
    if (refTableExport != null) {
      columnMap.put(KEY_REF_TABLE, refTableExport);
    }
    if (column.getRefLink() != null) {
      columnMap.put(KEY_REF_LINK, column.getRefLink());
    }
    if (column.getRefBack() != null) {
      columnMap.put(KEY_REF_BACK, column.getRefBack());
    }
    if (column.getRefLabel() != null) {
      columnMap.put(KEY_REF_LABEL, column.getRefLabel());
    }
    if (column.getKey() > 0) {
      columnMap.put(KEY_KEY, column.getKey());
    }
    putRequired(columnMap, column);
    if (Boolean.TRUE.equals(column.isReadonly())) {
      columnMap.put(KEY_READONLY, true);
    }
    if (column.getDefaultValue() != null) {
      columnMap.put(KEY_DEFAULT_VALUE, column.getDefaultValue());
    }
    putLocalized(columnMap, KEY_DESCRIPTION, column.getDescriptions());
    putLocalized(columnMap, KEY_LABEL, column.getLabels());
    if (column.getFormLabel() != null) {
      columnMap.put(KEY_FORM_LABEL, column.getFormLabel());
    }
    if (column.getValues() != null && !column.getValues().isEmpty()) {
      columnMap.put(KEY_VALUES, new ArrayList<>(column.getValues()));
    }
    if (column.getSemantics() != null && column.getSemantics().length > 0) {
      columnMap.put(KEY_SEMANTICS, List.of(column.getSemantics()));
    }
    if (column.getValidation() != null) {
      columnMap.put(KEY_VALIDATION, column.getValidation());
    }
    if (column.getVisible() != null) {
      columnMap.put(KEY_VISIBLE, column.getVisible());
    }
    if (column.getComputed() != null) {
      columnMap.put(KEY_COMPUTED, column.getComputed());
    }
    if (column.getProfiles() != null && column.getProfiles().length > 0) {
      columnMap.put(KEY_PROFILES, List.of(column.getProfiles()));
    }
    List<String> previous =
        previousNames.getOrDefault(column.getTableName(), Map.of()).get(column.getName());
    if (previous != null && !previous.isEmpty()) {
      columnMap.put(KEY_PREVIOUS_NAMES, new ArrayList<>(previous));
    }
    return columnMap;
  }

  private static Map<String, Object> headingToMap(
      Column column, String markerKey, String markerValue) {
    Map<String, Object> headingMap = new LinkedHashMap<>();
    String levelKey = ColumnType.SECTION.equals(column.getColumnType()) ? KEY_SECTION : KEY_HEADING;
    headingMap.put(levelKey, column.getName());
    if (markerKey != null) {
      headingMap.put(markerKey, markerValue);
    }
    putLocalized(headingMap, KEY_LABEL, column.getLabels());
    putLocalized(headingMap, KEY_DESCRIPTION, column.getDescriptions());
    if (column.getVisible() != null) {
      headingMap.put(KEY_VISIBLE, column.getVisible());
    }
    return headingMap;
  }

  private static String refTableExport(Column column) {
    String refTableName = column.getRefTableName();
    if (refTableName == null) {
      return null;
    }
    String refSchema = column.getRefSchemaName();
    if (refSchema != null && !refSchema.equals(column.getSchemaName())) {
      return refSchema + "." + refTableName;
    }
    return refTableName;
  }

  private static void putLocalized(
      Map<String, Object> target, String baseKey, Map<String, String> values) {
    String defaultValue = values.get(DEFAULT_LOCALE);
    if (defaultValue != null) {
      target.put(baseKey, defaultValue);
    }
    for (Map.Entry<String, String> entry : values.entrySet()) {
      if (!DEFAULT_LOCALE.equals(entry.getKey())) {
        target.put(baseKey + LOCALE_SUFFIX + entry.getKey(), entry.getValue());
      }
    }
  }

  private static void putRequired(Map<String, Object> columnMap, Column column) {
    String required = column.getRequired();
    if (required == null) {
      return;
    }
    if (BOOLEAN_TRUE.equalsIgnoreCase(required)) {
      columnMap.put(KEY_REQUIRED, Boolean.TRUE);
    } else if (BOOLEAN_FALSE.equalsIgnoreCase(required)) {
      columnMap.put(KEY_REQUIRED, Boolean.FALSE);
    } else {
      columnMap.put(KEY_REQUIRED, required);
    }
  }

  private static String dump(Object data) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(false);
    options.setIndent(2);
    options.setSplitLines(false);
    return new Yaml(options).dump(data);
  }
}
