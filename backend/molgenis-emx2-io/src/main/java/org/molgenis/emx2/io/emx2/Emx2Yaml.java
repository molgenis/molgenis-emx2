package org.molgenis.emx2.io.emx2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * mapping in {@link #parseTableFile} / {@link #columnToMap} for inheritance, reuse, dotted refs and
 * i18n; the bundle envelope handling here stays unchanged.
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

  private static final String DEFAULT_LOCALE = "en";
  private static final String BOOLEAN_TRUE = "true";
  private static final String BOOLEAN_FALSE = "false";

  private static final Set<String> BUNDLE_KEYS =
      Set.of(KEY_FORMAT_VERSION, KEY_VERSION, KEY_TABLES, KEY_SETTINGS);
  private static final Set<String> TABLE_ENTRY_KEYS = Set.of(KEY_FILE);
  private static final Set<String> TABLE_KEYS =
      Set.of(KEY_NAME, KEY_LABEL, KEY_DESCRIPTION, KEY_SETTINGS, KEY_COLUMNS);
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
          KEY_FORM_LABEL);

  private Emx2Yaml() {}

  public static Emx2YamlBundle fromBundle(Path bundlePathOrDir) throws IOException {
    Path directory =
        Files.isDirectory(bundlePathOrDir) ? bundlePathOrDir : bundlePathOrDir.getParent();
    Path molgenisYaml = directory.resolve(MOLGENIS_YAML);
    if (!Files.exists(molgenisYaml)) {
      throw new MolgenisException("Bundle is missing '" + MOLGENIS_YAML + "' at: " + directory);
    }
    Map<String, String> files = new LinkedHashMap<>();
    files.put(MOLGENIS_YAML, Files.readString(molgenisYaml));
    for (String relativePath : tableFilePaths(files.get(MOLGENIS_YAML))) {
      files.put(relativePath, Files.readString(directory.resolve(relativePath)));
    }
    return fromBundleFiles(files);
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
    parseTables(root.get(KEY_TABLES), files, schema);

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
      Node tablesNode, Map<String, String> files, SchemaMetadata schema) {
    if (tablesNode == null) {
      return;
    }
    YamlDocumentReader reader = new YamlDocumentReader(MOLGENIS_YAML);
    List<Node> entries = reader.sequence(tablesNode, KEY_TABLES);
    for (int index = 0; index < entries.size(); index++) {
      LinkedHashMap<String, Node> entry =
          reader.mapping(entries.get(index), KEY_TABLES + "[" + index + "]", TABLE_ENTRY_KEYS);
      String relativePath =
          reader.scalar(entry.get(KEY_FILE), KEY_TABLES + "[" + index + "]." + KEY_FILE);
      String content = files.get(relativePath);
      if (content == null) {
        throw reader.error("referenced table file not found: " + relativePath, entry.get(KEY_FILE));
      }
      parseTableFile(relativePath, content, schema);
    }
  }

  private static void parseTableFile(String fileLabel, String content, SchemaMetadata schema) {
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
    TableMetadata table = new TableMetadata(tableName);

    if (tableMap.containsKey(KEY_LABEL)) {
      table.setLabel(reader.scalar(tableMap.get(KEY_LABEL), tableName + "." + KEY_LABEL));
    }
    if (tableMap.containsKey(KEY_DESCRIPTION)) {
      table.setDescription(
          reader.scalar(tableMap.get(KEY_DESCRIPTION), tableName + "." + KEY_DESCRIPTION));
    }
    if (tableMap.containsKey(KEY_SETTINGS)) {
      table.setSettings(
          reader.scalarMapping(tableMap.get(KEY_SETTINGS), tableName + "." + KEY_SETTINGS));
    }
    parseColumns(reader, tableMap.get(KEY_COLUMNS), tableName, table);

    schema.create(table);
  }

  private static void parseColumns(
      YamlDocumentReader reader, Node columnsNode, String tableName, TableMetadata table) {
    if (columnsNode == null) {
      return;
    }
    List<Node> entries = reader.sequence(columnsNode, tableName + "." + KEY_COLUMNS);
    for (int index = 0; index < entries.size(); index++) {
      String path = tableName + " > " + KEY_COLUMNS + "[" + index + "]";
      LinkedHashMap<String, Node> columnMap = reader.mapping(entries.get(index), path, COLUMN_KEYS);
      table.add(parseColumn(reader, columnMap, path, index));
    }
  }

  private static Column parseColumn(
      YamlDocumentReader reader, LinkedHashMap<String, Node> columnMap, String path, int position) {
    Node nameNode = columnMap.get(KEY_NAME);
    if (nameNode == null) {
      throw new MolgenisException(reader.getFileLabel() + ": column is missing '" + KEY_NAME + "'");
    }
    Column column = new Column(reader.scalar(nameNode, path + "." + KEY_NAME));
    column.setPosition(position);

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
      files.put(relativePath, dump(tableToMap(table)));
    }
    root.put(KEY_TABLES, tableEntries);

    if (!schema.getSettings().isEmpty()) {
      root.put(KEY_SETTINGS, new LinkedHashMap<>(schema.getSettings()));
    }

    files.put(MOLGENIS_YAML, dump(root));
    return files;
  }

  private static Map<String, Object> tableToMap(TableMetadata table) {
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
    List<Map<String, Object>> columns = new ArrayList<>();
    for (Column column : table.getColumns()) {
      if (column.isSystemColumn()) {
        continue;
      }
      columns.add(columnToMap(column));
    }
    if (!columns.isEmpty()) {
      tableMap.put(KEY_COLUMNS, columns);
    }
    return tableMap;
  }

  private static Map<String, Object> columnToMap(Column column) {
    Map<String, Object> columnMap = new LinkedHashMap<>();
    columnMap.put(KEY_NAME, column.getName());
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
