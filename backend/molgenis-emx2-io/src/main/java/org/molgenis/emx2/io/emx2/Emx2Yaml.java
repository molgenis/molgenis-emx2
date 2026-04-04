package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.molgenis.emx2.*;

public class Emx2Yaml {

  private static final String FIELD_TABLE = "table";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_PROFILES = "profiles";
  private static final String FIELD_EXTENSIONS = "extensions";
  private static final String FIELD_SECTIONS = "sections";
  private static final String FIELD_COLUMNS = "columns";
  private static final String FIELD_EXTENSION = "extension";
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

  private Emx2Yaml() {
    // hidden
  }

  @SuppressWarnings("unchecked")
  public static SchemaMetadata fromYamlFile(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> yaml = mapper.readValue(inputStream, Map.class);

    String rootTableName = (String) yaml.get(FIELD_TABLE);
    if (rootTableName == null) {
      throw new MolgenisException("YAML parse error: missing required 'table' field");
    }
    TableMetadata rootTable = new TableMetadata(rootTableName);
    applyTableDescription(rootTable, yaml);

    Map<String, TableMetadata> extensionTables = new LinkedHashMap<>();
    List<Map<String, Object>> extensions =
        (List<Map<String, Object>>) yaml.getOrDefault(FIELD_EXTENSIONS, List.of());
    for (Map<String, Object> ext : extensions) {
      String extName = (String) ext.get(FIELD_NAME);
      TableMetadata extTable = new TableMetadata(extName);

      List<String> inherits = (List<String>) ext.get(FIELD_INHERITS);
      if (inherits != null && !inherits.isEmpty()) {
        extTable.setInheritNames(inherits.toArray(new String[0]));
      } else {
        extTable.setInheritNames(rootTableName);
      }

      if (Boolean.TRUE.equals(ext.get(FIELD_INTERNAL))) {
        extTable.setTableType(TableType.INTERNAL);
      }
      applyTableDescription(extTable, ext);
      extensionTables.put(extName, extTable);
    }

    List<Map<String, Object>> sections =
        (List<Map<String, Object>>) yaml.getOrDefault(FIELD_SECTIONS, List.of());
    for (Map<String, Object> section : sections) {
      String sectionExtension = (String) section.get(FIELD_EXTENSION);
      List<Map<String, Object>> columns =
          (List<Map<String, Object>>) section.getOrDefault(FIELD_COLUMNS, List.of());
      for (Map<String, Object> colDef : columns) {
        Column column = buildColumn(colDef);
        String colExtension =
            colDef.containsKey(FIELD_EXTENSION)
                ? (String) colDef.get(FIELD_EXTENSION)
                : sectionExtension;
        if (colExtension != null) {
          TableMetadata extTable = extensionTables.get(colExtension);
          if (extTable == null) {
            throw new MolgenisException(
                "YAML parse error in table '"
                    + rootTableName
                    + "': column '"
                    + column.getName()
                    + "' references unknown extension '"
                    + colExtension
                    + "'");
          }
          extTable.add(column);
        } else {
          rootTable.add(column);
        }
      }
    }

    SchemaMetadata schema = new SchemaMetadata();
    schema.create(rootTable);
    for (TableMetadata extTable : extensionTables.values()) {
      schema.create(extTable);
    }
    return schema;
  }

  public static SchemaMetadata fromYamlDirectory(Path directory) throws IOException {
    SchemaMetadata schema = new SchemaMetadata();
    Path tablesDir = directory.resolve(TABLES_DIR);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(tablesDir, "*.yaml")) {
      for (Path yamlFile : stream) {
        try (InputStream inputStream = Files.newInputStream(yamlFile)) {
          SchemaMetadata fileSchema = fromYamlFile(inputStream);
          for (TableMetadata table : fileSchema.getTables()) {
            schema.create(table);
          }
        }
      }
    }
    return schema;
  }

  public static String toYamlFile(SchemaMetadata schema, String rootTableName) throws IOException {
    TableMetadata rootTable = schema.getTableMetadata(rootTableName);
    if (rootTable == null) {
      throw new MolgenisException("Table not found: " + rootTableName);
    }

    List<TableMetadata> extensions = findExtensionTables(schema, rootTableName);

    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(FIELD_TABLE, rootTableName);
    if (rootTable.getDescription() != null) {
      doc.put(FIELD_DESCRIPTION, rootTable.getDescription());
    }
    if (rootTable.getProfiles() != null && rootTable.getProfiles().length > 0) {
      doc.put(FIELD_PROFILES, Arrays.asList(rootTable.getProfiles()));
    }
    if (rootTable.getSemantics() != null && rootTable.getSemantics().length > 0) {
      doc.put(FIELD_SEMANTICS, Arrays.asList(rootTable.getSemantics()));
    }

    if (!extensions.isEmpty()) {
      List<Map<String, Object>> extList = new ArrayList<>();
      for (TableMetadata ext : extensions) {
        Map<String, Object> extMap = new LinkedHashMap<>();
        extMap.put(FIELD_NAME, ext.getTableName());
        if (ext.getDescription() != null) {
          extMap.put(FIELD_DESCRIPTION, ext.getDescription());
        }
        String[] inheritNames = ext.getInheritNames();
        if (inheritNames != null
            && inheritNames.length > 0
            && !(inheritNames.length == 1 && rootTableName.equals(inheritNames[0]))) {
          extMap.put(FIELD_INHERITS, Arrays.asList(inheritNames));
        }
        if (TableType.INTERNAL.equals(ext.getTableType())) {
          extMap.put(FIELD_INTERNAL, true);
        }
        if (ext.getProfiles() != null && ext.getProfiles().length > 0) {
          extMap.put(FIELD_PROFILES, Arrays.asList(ext.getProfiles()));
        }
        if (ext.getSemantics() != null && ext.getSemantics().length > 0) {
          extMap.put(FIELD_SEMANTICS, Arrays.asList(ext.getSemantics()));
        }
        String extLabel = ext.getLabel();
        if (extLabel != null && !extLabel.equals(ext.getTableName())) {
          extMap.put(FIELD_LABEL, extLabel);
        }
        extList.add(extMap);
      }
      doc.put(FIELD_EXTENSIONS, extList);
    }

    List<Map<String, Object>> sections = new ArrayList<>();
    List<Map<String, Object>> rootColumns = buildColumnMaps(rootTable.getNonInheritedColumns());
    if (!rootColumns.isEmpty()) {
      Map<String, Object> rootSection = new LinkedHashMap<>();
      rootSection.put(FIELD_NAME, rootTableName);
      rootSection.put(FIELD_COLUMNS, rootColumns);
      sections.add(rootSection);
    }
    for (TableMetadata ext : extensions) {
      List<Map<String, Object>> extColumns = buildColumnMaps(ext.getNonInheritedColumns());
      if (!extColumns.isEmpty()) {
        Map<String, Object> extSection = new LinkedHashMap<>();
        extSection.put(FIELD_NAME, ext.getTableName());
        extSection.put(FIELD_EXTENSION, ext.getTableName());
        extSection.put(FIELD_COLUMNS, extColumns);
        sections.add(extSection);
      }
    }
    if (!sections.isEmpty()) {
      doc.put(FIELD_SECTIONS, sections);
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.writeValueAsString(doc);
  }

  public static String toYamlSchema(SchemaMetadata schema) throws IOException {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (TableMetadata table : schema.getTables()) {
      if (table.getInheritNames() == null || table.getInheritNames().length == 0) {
        if (!first) {
          sb.append("---\n");
        }
        sb.append(toYamlFile(schema, table.getTableName()));
        first = false;
      }
    }
    return sb.toString();
  }

  public static SchemaMetadata fromYamlSchema(String yaml) throws IOException {
    SchemaMetadata schema = new SchemaMetadata();
    String[] documents = yaml.split("(?m)^---\\s*$");
    for (String doc : documents) {
      String trimmed = doc.trim();
      if (!trimmed.isEmpty()) {
        SchemaMetadata fileSchema =
            fromYamlFile(new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8)));
        for (TableMetadata table : fileSchema.getTables()) {
          schema.create(table);
        }
      }
    }
    return schema;
  }

  public static void toYamlDirectory(SchemaMetadata schema, Path directory) throws IOException {
    Path tablesDir = directory.resolve(TABLES_DIR);
    Files.createDirectories(tablesDir);
    for (TableMetadata table : schema.getTables()) {
      if (isRootTable(table)) {
        String yaml = toYamlFile(schema, table.getTableName());
        Files.writeString(tablesDir.resolve(table.getTableName() + ".yaml"), yaml);
      }
    }
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
  private static List<Map<String, Object>> buildColumnMaps(List<Column> columns) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Column col : columns) {
      if (col.isSystemColumn()) {
        continue;
      }
      Map<String, Object> colMap = new LinkedHashMap<>();
      colMap.put(FIELD_NAME, col.getName());
      if (!ColumnType.STRING.equals(col.getColumnType())) {
        colMap.put(FIELD_TYPE, col.getColumnType().toString().toLowerCase());
      }
      if (col.getKey() > 0) {
        colMap.put(FIELD_KEY, col.getKey());
      }
      if (col.getRequired() != null) {
        colMap.put(FIELD_REQUIRED, col.getRequired());
      }
      if (col.getDefaultValue() != null) {
        colMap.put(FIELD_DEFAULT_VALUE, col.getDefaultValue());
      }
      if (col.getRefTableName() != null) {
        colMap.put(FIELD_REF_TABLE, col.getRefTableName());
      }
      if (col.getRefBack() != null) {
        colMap.put(FIELD_REF_BACK, col.getRefBack());
      }
      if (col.getRefLink() != null) {
        colMap.put(FIELD_REF_LINK, col.getRefLink());
      }
      if (col.getRefLabel() != null) {
        colMap.put(FIELD_REF_LABEL, col.getRefLabel());
      }
      if (col.getSemantics() != null && col.getSemantics().length > 0) {
        colMap.put(FIELD_SEMANTICS, Arrays.asList(col.getSemantics()));
      }
      if (col.getProfiles() != null && col.getProfiles().length > 0) {
        colMap.put(FIELD_PROFILES, Arrays.asList(col.getProfiles()));
      }
      if (col.getValidation() != null) {
        colMap.put(FIELD_VALIDATION, col.getValidation());
      }
      if (col.getVisible() != null) {
        colMap.put(FIELD_VISIBLE, col.getVisible());
      }
      if (col.getComputed() != null) {
        colMap.put(FIELD_COMPUTED, col.getComputed());
      }
      if (Boolean.TRUE.equals(col.isReadonly())) {
        colMap.put(FIELD_READONLY, true);
      }
      String colDescription = col.getDescriptions().get("en");
      if (colDescription != null) {
        colMap.put(FIELD_DESCRIPTION, colDescription);
      }
      String colLabel = col.getLabel();
      if (colLabel != null && !colLabel.equals(col.getName())) {
        colMap.put(FIELD_LABEL, colLabel);
      }
      result.add(colMap);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static Column buildColumn(Map<String, Object> colDef) {
    String name = (String) colDef.get(FIELD_NAME);
    Column column = new Column(name);

    String typeStr = (String) colDef.get(FIELD_TYPE);
    if (typeStr != null) {
      try {
        column.setType(ColumnType.valueOf(typeStr.toUpperCase().replace(" ", "_")));
      } catch (IllegalArgumentException e) {
        throw new MolgenisException(
            "YAML parse error: unknown type '" + typeStr + "' for column '" + name + "'");
      }
    }

    Object key = colDef.get(FIELD_KEY);
    if (key != null) {
      column.setKey(((Number) key).intValue());
    }

    Object required = colDef.get(FIELD_REQUIRED);
    if (required != null) {
      if (required instanceof Boolean) {
        column.setRequired((Boolean) required);
      } else {
        column.setRequired(required.toString());
      }
    }

    String defaultValue = (String) colDef.get(FIELD_DEFAULT_VALUE);
    if (defaultValue != null) {
      column.setDefaultValue(defaultValue);
    }

    String refTable = (String) colDef.get(FIELD_REF_TABLE);
    if (refTable != null) {
      column.setRefTable(refTable);
    }

    String refBack = (String) colDef.get(FIELD_REF_BACK);
    if (refBack != null) {
      column.setRefBack(refBack);
    }

    String refLink = (String) colDef.get(FIELD_REF_LINK);
    if (refLink != null) {
      column.setRefLink(refLink);
    }

    String refLabel = (String) colDef.get(FIELD_REF_LABEL);
    if (refLabel != null) {
      column.setRefLabel(refLabel);
    }

    String refSchema = (String) colDef.get(FIELD_REF_SCHEMA);
    if (refSchema != null) {
      column.setRefSchemaName(refSchema);
    }

    String computed = (String) colDef.get(FIELD_COMPUTED);
    if (computed != null) {
      column.setComputed(computed);
    }

    Object readonly = colDef.get(FIELD_READONLY);
    if (readonly != null) {
      if (readonly instanceof Boolean) {
        column.setReadonly((Boolean) readonly);
      } else {
        column.setReadonly(Boolean.parseBoolean(readonly.toString()));
      }
    }

    String label = (String) colDef.get(FIELD_LABEL);
    if (label != null) {
      column.setLabel(label);
    }

    Object position = colDef.get(FIELD_POSITION);
    if (position != null) {
      column.setPosition(((Number) position).intValue());
    }

    String oldName = (String) colDef.get(FIELD_OLD_NAME);
    if (oldName != null) {
      column.setOldName(oldName);
    }

    Object drop = colDef.get(FIELD_DROP);
    if (Boolean.TRUE.equals(drop)) {
      column.drop();
    }

    Object semantics = colDef.get(FIELD_SEMANTICS);
    if (semantics instanceof List) {
      List<String> semList = (List<String>) semantics;
      column.setSemantics(semList.toArray(new String[0]));
    } else if (semantics instanceof String) {
      column.setSemantics((String) semantics);
    }

    Object profiles = colDef.get(FIELD_PROFILES);
    if (profiles instanceof List) {
      List<String> profList = (List<String>) profiles;
      column.setProfiles(profList.toArray(new String[0]));
    } else if (profiles instanceof String) {
      column.setProfiles((String) profiles);
    }

    String validation = (String) colDef.get(FIELD_VALIDATION);
    if (validation != null) {
      column.setValidation(validation);
    }

    String visible = (String) colDef.get(FIELD_VISIBLE);
    if (visible != null) {
      column.setVisible(visible);
    }

    String description = (String) colDef.get(FIELD_DESCRIPTION);
    if (description != null) {
      column.setDescription(description);
    }

    return column;
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
        try (InputStream inputStream = Files.newInputStream(yamlFile)) {
          SchemaMetadata fileSchema = fromYamlFile(inputStream);
          for (TableMetadata table : fileSchema.getTables()) {
            schema.create(table);
          }
        }
      }
    }

    List<String> profiles = (List<String>) yaml.getOrDefault(FIELD_PROFILES, List.of());

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
          try (InputStream inputStream = Files.newInputStream(yamlFile)) {
            SchemaMetadata fileSchema = fromYamlFile(inputStream);
            for (TableMetadata table : fileSchema.getTables()) {
              fixedSchema.create(table);
            }
          }
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
        name, description, schema, profiles, settings, permissions, fixedSchemas);
  }

  private static List<Path> resolveImport(Path baseDir, String importEntry) throws IOException {
    if (importEntry.endsWith("/*")) {
      String dirPart = importEntry.substring(0, importEntry.length() - 2);
      Path dir = baseDir.resolve(dirPart).normalize();
      List<Path> result = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.yaml")) {
        for (Path path : stream) {
          result.add(path);
        }
      }
      return result;
    } else {
      return List.of(baseDir.resolve(importEntry).normalize());
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
    if (!result.getProfiles().isEmpty()) {
      doc.put(FIELD_PROFILES, result.getProfiles());
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
    private final List<String> profiles;
    private final Map<String, String> settings;
    private final Map<String, String> permissions;
    private final List<FixedSchema> fixedSchemas;

    public TemplateResult(
        String name,
        String description,
        SchemaMetadata schema,
        List<String> profiles,
        Map<String, String> settings,
        Map<String, String> permissions,
        List<FixedSchema> fixedSchemas) {
      this.name = name;
      this.description = description;
      this.schema = schema;
      this.profiles = profiles;
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

    public List<String> getProfiles() {
      return profiles;
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
    Object profiles = source.get(FIELD_PROFILES);
    if (profiles instanceof List) {
      List<String> profList = (List<String>) profiles;
      table.setProfiles(profList.toArray(new String[0]));
    } else if (profiles instanceof String) {
      table.setProfiles((String) profiles);
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
