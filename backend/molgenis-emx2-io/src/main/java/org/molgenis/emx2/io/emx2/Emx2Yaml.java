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
import java.util.PriorityQueue;
import java.util.Set;
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
  private static final String BOOLEAN_TRUE = "true";
  private static final String BOOLEAN_FALSE = "false";

  private static final Set<String> BUNDLE_KEYS =
      Set.of(KEY_FORMAT_VERSION, KEY_VERSION, KEY_TABLES, KEY_SETTINGS, KEY_IMPORTS);
  private static final Set<String> TABLE_ENTRY_KEYS = Set.of(KEY_FILE);
  private static final Set<String> TABLE_KEYS =
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
          KEY_IMPORTS);
  private static final Set<String> SHARED_FILE_KEYS = Set.of(KEY_COLUMNS);
  private static final Set<String> HEADING_KEYS =
      Set.of(
          KEY_SECTION,
          KEY_HEADING,
          KEY_VISIBLE,
          KEY_LABEL,
          KEY_DESCRIPTION,
          KEY_SUBCLASS,
          KEY_MODULE);
  private static final Set<String> SUBTABLE_KEYS =
      Set.of(KEY_NAME, KEY_EXTENDS, KEY_LABEL, KEY_DESCRIPTION, KEY_TABLE_TYPE);
  private static final Set<String> COLUMN_KEYS =
      Set.of(
          KEY_NAME,
          KEY_TYPE,
          KEY_KEY,
          KEY_REQUIRED,
          KEY_READONLY,
          KEY_DEFAULT_VALUE,
          KEY_VALIDATION,
          KEY_VISIBLE,
          KEY_COMPUTED,
          KEY_SEMANTICS,
          KEY_VALUES,
          KEY_DESCRIPTION,
          KEY_LABEL,
          KEY_FORM_LABEL,
          KEY_SUBCLASS,
          KEY_MODULE);

  private Emx2Yaml() {}

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
    for (String relativePath : tableFilePaths(rootContent)) {
      String tableContent = Files.readString(directory.resolve(relativePath));
      files.put(relativePath, tableContent);
      readImports(tableContent, "table", TABLE_KEYS, directory, files);
    }
    return fromBundleFiles(files);
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
    List<SharedFile> bundleImports =
        loadSharedFiles(reader, root.get(KEY_IMPORTS), files, "bundle." + KEY_IMPORTS);
    parseTables(root.get(KEY_TABLES), files, schema, bundleImports);

    String version =
        root.containsKey(KEY_VERSION) ? reader.scalar(root.get(KEY_VERSION), KEY_VERSION) : null;
    return new Emx2YamlBundle(schema, formatVersion, version);
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

  private static List<String> tableFilePaths(String rootContent) {
    YamlDocumentReader reader = new YamlDocumentReader(MOLGENIS_YAML);
    Node rootNode = reader.compose(rootContent);
    if (rootNode == null) {
      throw new MolgenisException(MOLGENIS_YAML + " is empty");
    }
    LinkedHashMap<String, Node> root = reader.mapping(rootNode, "bundle", BUNDLE_KEYS);
    List<String> paths = new ArrayList<>();
    Node tablesNode = root.get(KEY_TABLES);
    if (tablesNode == null) {
      return paths;
    }
    List<Node> entries = reader.sequence(tablesNode, KEY_TABLES);
    for (int index = 0; index < entries.size(); index++) {
      LinkedHashMap<String, Node> entry =
          reader.mapping(entries.get(index), KEY_TABLES + "[" + index + "]", TABLE_ENTRY_KEYS);
      Node fileNode = entry.get(KEY_FILE);
      if (fileNode == null) {
        throw reader.error("table entry is missing '" + KEY_FILE + "'", entries.get(index));
      }
      paths.add(reader.scalar(fileNode, KEY_TABLES + "[" + index + "]." + KEY_FILE));
    }
    return paths;
  }

  private static void parseTables(
      Node tablesNode,
      Map<String, String> files,
      SchemaMetadata schema,
      List<SharedFile> bundleImports) {
    if (tablesNode == null) {
      return;
    }
    YamlDocumentReader reader = new YamlDocumentReader(MOLGENIS_YAML);
    List<Node> entries = reader.sequence(tablesNode, KEY_TABLES);
    List<ParsedFile> parsedFiles = new ArrayList<>();
    for (int index = 0; index < entries.size(); index++) {
      LinkedHashMap<String, Node> entry =
          reader.mapping(entries.get(index), KEY_TABLES + "[" + index + "]", TABLE_ENTRY_KEYS);
      String relativePath =
          reader.scalar(entry.get(KEY_FILE), KEY_TABLES + "[" + index + "]." + KEY_FILE);
      String content = files.get(relativePath);
      if (content == null) {
        throw reader.error("referenced table file not found: " + relativePath, entry.get(KEY_FILE));
      }
      parsedFiles.add(parseHierarchyFile(relativePath, content, files, bundleImports));
    }
    assemble(parsedFiles, schema);
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
    Node nameNode = tableMap.get(KEY_NAME);
    if (nameNode == null) {
      throw reader.error("table file is missing '" + KEY_NAME + "'", fileNode);
    }
    String tableName = reader.scalar(nameNode, KEY_NAME);
    TableMetadata primary = new TableMetadata(tableName);

    if (tableMap.containsKey(KEY_LABEL)) {
      primary.setLabel(reader.scalar(tableMap.get(KEY_LABEL), tableName + "." + KEY_LABEL));
    }
    if (tableMap.containsKey(KEY_DESCRIPTION)) {
      primary.setDescription(
          reader.scalar(tableMap.get(KEY_DESCRIPTION), tableName + "." + KEY_DESCRIPTION));
    }
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
      if (entry.containsKey(KEY_LABEL)) {
        subtable.setLabel(reader.scalar(entry.get(KEY_LABEL), path + "." + KEY_LABEL));
      }
      if (entry.containsKey(KEY_DESCRIPTION)) {
        subtable.setDescription(
            reader.scalar(entry.get(KEY_DESCRIPTION), path + "." + KEY_DESCRIPTION));
      }
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
    return new ColumnEntry(target, column);
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
    if (headingMap.containsKey(KEY_LABEL)) {
      column.setLabel(reader.scalar(headingMap.get(KEY_LABEL), path + "." + KEY_LABEL));
    }
    if (headingMap.containsKey(KEY_DESCRIPTION)) {
      column.setDescription(
          reader.scalar(headingMap.get(KEY_DESCRIPTION), path + "." + KEY_DESCRIPTION));
    }
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
    if (columnMap.containsKey(KEY_DESCRIPTION)) {
      column.setDescription(
          reader.scalar(columnMap.get(KEY_DESCRIPTION), path + "." + KEY_DESCRIPTION));
    }
    if (columnMap.containsKey(KEY_LABEL)) {
      column.setLabel(reader.scalar(columnMap.get(KEY_LABEL), path + "." + KEY_LABEL));
    }
    if (columnMap.containsKey(KEY_FORM_LABEL)) {
      column.setFormLabel(
          reader.scalar(columnMap.get(KEY_FORM_LABEL), path + "." + KEY_FORM_LABEL));
    }
    return column;
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

  private static void assemble(List<ParsedFile> parsedFiles, SchemaMetadata schema) {
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

  private record ColumnEntry(String targetTable, Column column) {}

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

    List<Map<String, Object>> tableEntries = new ArrayList<>();
    List<TableMetadata> rootTables = schema.getRootTables();
    for (TableMetadata table : rootTables) {
      if (TableType.ONTOLOGIES.equals(table.getTableType())) {
        continue;
      }
      String relativePath = TABLES_DIR + table.getTableName() + ".yaml";
      tableEntries.add(new LinkedHashMap<>(Map.of(KEY_FILE, relativePath)));
      files.put(relativePath, dump(tableToMap(table, schema)));
    }
    root.put(KEY_TABLES, tableEntries);

    if (!schema.getSettings().isEmpty()) {
      root.put(KEY_SETTINGS, new LinkedHashMap<>(schema.getSettings()));
    }

    files.put(MOLGENIS_YAML, dump(root));
    return files;
  }

  private static Map<String, Object> tableToMap(TableMetadata table, SchemaMetadata schema) {
    Map<String, Object> tableMap = new LinkedHashMap<>();
    tableMap.put(KEY_NAME, table.getTableName());
    String label = table.getLabels().get(DEFAULT_LOCALE);
    if (label != null) {
      tableMap.put(KEY_LABEL, label);
    }
    String description = table.getDescriptions().get(DEFAULT_LOCALE);
    if (description != null) {
      tableMap.put(KEY_DESCRIPTION, description);
    }
    if (!table.getSettings().isEmpty()) {
      tableMap.put(KEY_SETTINGS, new LinkedHashMap<>(table.getSettings()));
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

    List<Map<String, Object>> columns = wovenColumns(table, subclasses, modules);
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
      String label = subtable.getLabels().get(DEFAULT_LOCALE);
      if (label != null) {
        entry.put(KEY_LABEL, label);
      }
      String description = subtable.getDescriptions().get(DEFAULT_LOCALE);
      if (description != null) {
        entry.put(KEY_DESCRIPTION, description);
      }
      block.add(entry);
    }
    return block;
  }

  private static List<Map<String, Object>> wovenColumns(
      TableMetadata root, List<TableMetadata> subclasses, List<TableMetadata> modules) {
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
      columns.add(columnToMap(export.column(), export.markerKey(), export.markerValue()));
    }
    return columns;
  }

  private static List<Column> ownColumns(TableMetadata table) {
    return table.getNonInheritedColumns().stream()
        .filter(column -> !column.isSystemColumn())
        .toList();
  }

  private static Map<String, Object> columnToMap(
      Column column, String markerKey, String markerValue) {
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
    String description = column.getDescriptions().get(DEFAULT_LOCALE);
    if (description != null) {
      columnMap.put(KEY_DESCRIPTION, description);
    }
    String label = column.getLabels().get(DEFAULT_LOCALE);
    if (label != null) {
      columnMap.put(KEY_LABEL, label);
    }
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
    String label = column.getLabels().get(DEFAULT_LOCALE);
    if (label != null) {
      headingMap.put(KEY_LABEL, label);
    }
    String description = column.getDescriptions().get(DEFAULT_LOCALE);
    if (description != null) {
      headingMap.put(KEY_DESCRIPTION, description);
    }
    if (column.getVisible() != null) {
      headingMap.put(KEY_VISIBLE, column.getVisible());
    }
    return headingMap;
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
