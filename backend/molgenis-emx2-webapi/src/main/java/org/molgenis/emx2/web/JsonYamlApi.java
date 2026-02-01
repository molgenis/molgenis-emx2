package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.MG_DRAFT;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.DownloadApiUtils.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.utils.TypeUtils;

public class JsonYamlApi {

  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  private JsonYamlApi() {}

  private enum OutputFormat {
    JSON(ACCEPT_JSON, ".json", JSON_MAPPER),
    YAML(ACCEPT_YAML, ".yaml", YAML_MAPPER);

    private final String contentType;
    private final String extension;
    private final ObjectMapper mapper;

    OutputFormat(String contentType, String extension, ObjectMapper mapper) {
      this.contentType = contentType;
      this.extension = extension;
      this.mapper = mapper;
    }

    String contentType() {
      return contentType;
    }

    String extension() {
      return extension;
    }

    ObjectMapper mapper() {
      return mapper;
    }
  }

  public static void create(Javalin app) {
    final String jsonApi = "/{schema}/api/json/";
    app.get(jsonApi + "_schema", ctx -> getSchemaMetadata(ctx, OutputFormat.JSON));
    app.post(jsonApi + "_schema", ctx -> postSchema(ctx, OutputFormat.JSON));
    app.delete(jsonApi + "_schema", ctx -> deleteSchema(ctx, OutputFormat.JSON));
    app.get(jsonApi + "_data", ctx -> getData(ctx, OutputFormat.JSON));
    app.get(jsonApi + "_all", ctx -> getAll(ctx, OutputFormat.JSON));
    app.get(jsonApi + "_members", ctx -> getMembers(ctx, OutputFormat.JSON));
    app.get(jsonApi + "_settings", ctx -> getSettings(ctx, OutputFormat.JSON));
    app.get(jsonApi + "_changelog", ctx -> getChangelog(ctx, OutputFormat.JSON));
    app.get(jsonApi + "{table}", ctx -> getTable(ctx, OutputFormat.JSON));
    app.post(jsonApi + "{table}", ctx -> postTable(ctx, OutputFormat.JSON));
    app.delete(jsonApi + "{table}", ctx -> deleteTable(ctx, OutputFormat.JSON));
    app.get(jsonApi + "{table}/*", ctx -> getRow(ctx, OutputFormat.JSON));
    app.put(jsonApi + "{table}/*", ctx -> putRow(ctx, OutputFormat.JSON));
    app.delete(jsonApi + "{table}/*", ctx -> deleteRow(ctx, OutputFormat.JSON));

    final String yamlApi = "/{schema}/api/yaml/";
    app.get(yamlApi + "_schema", ctx -> getSchemaMetadata(ctx, OutputFormat.YAML));
    app.post(yamlApi + "_schema", ctx -> postSchema(ctx, OutputFormat.YAML));
    app.delete(yamlApi + "_schema", ctx -> deleteSchema(ctx, OutputFormat.YAML));
    app.get(yamlApi + "_data", ctx -> getData(ctx, OutputFormat.YAML));
    app.get(yamlApi + "_all", ctx -> getAll(ctx, OutputFormat.YAML));
    app.get(yamlApi + "_members", ctx -> getMembers(ctx, OutputFormat.YAML));
    app.get(yamlApi + "_settings", ctx -> getSettings(ctx, OutputFormat.YAML));
    app.get(yamlApi + "_changelog", ctx -> getChangelog(ctx, OutputFormat.YAML));
    app.get(yamlApi + "{table}", ctx -> getTable(ctx, OutputFormat.YAML));
    app.post(yamlApi + "{table}", ctx -> postTable(ctx, OutputFormat.YAML));
    app.delete(yamlApi + "{table}", ctx -> deleteTable(ctx, OutputFormat.YAML));
    app.get(yamlApi + "{table}/*", ctx -> getRow(ctx, OutputFormat.YAML));
    app.put(yamlApi + "{table}/*", ctx -> putRow(ctx, OutputFormat.YAML));
    app.delete(yamlApi + "{table}/*", ctx -> deleteRow(ctx, OutputFormat.YAML));
  }

  private static void getSchemaMetadata(Context ctx, OutputFormat format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    String output =
        format == OutputFormat.JSON
            ? JsonUtil.schemaToJson(schema.getMetadata(), true)
            : schemaToYaml(schema.getMetadata(), true);
    ctx.contentType(format.contentType());
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_" + date + format.extension() + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void postSchema(Context ctx, OutputFormat format) throws Exception {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    SchemaMetadata otherSchema =
        format == OutputFormat.JSON ? jsonToSchema(ctx.body()) : yamlToSchema(ctx.body());
    timedOperation(
        ctx, "{ \"message\": \"add/update metadata success\" }", () -> schema.migrate(otherSchema));
  }

  private static void deleteSchema(Context ctx, OutputFormat format) throws Exception {
    SchemaMetadata schemaMetadata =
        format == OutputFormat.JSON ? jsonToSchema(ctx.body()) : yamlToSchema(ctx.body());
    timedOperation(
        ctx,
        "{ \"message\": \"removed metadata items success\" }",
        () -> MolgenisWebservice.getSchema(ctx).discard(schemaMetadata));
  }

  private static void getData(Context ctx, OutputFormat format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    boolean includeSystem = includeSystemColumns(ctx);

    Map<String, Object> dataMap = new LinkedHashMap<>();
    boolean hasViewPermission =
        schema.getInheritedRolesForActiveUser().contains(Privileges.VIEWER.toString());

    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      if (hasViewPermission || table.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
        List<Row> rows = table.query().retrieveRows();
        List<Map<String, Object>> tableData = convertRowsToMaps(rows, table, includeSystem);
        dataMap.put(tableName, tableData);
      }
    }

    String output = format.mapper().writeValueAsString(dataMap);
    ctx.contentType(format.contentType());
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_data_" + date + format.extension() + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void getAll(Context ctx, OutputFormat format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    boolean includeSystem = includeSystemColumns(ctx);

    Map<String, Object> allMap = new LinkedHashMap<>();

    String schemaJson =
        format == OutputFormat.JSON
            ? JsonUtil.schemaToJson(schema.getMetadata(), true)
            : schemaToYaml(schema.getMetadata(), true);
    Object schemaData = format.mapper().readValue(schemaJson, Object.class);
    allMap.put("schema", schemaData);

    Map<String, Object> dataMap = new LinkedHashMap<>();
    boolean hasViewPermission =
        schema.getInheritedRolesForActiveUser().contains(Privileges.VIEWER.toString());

    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      if (hasViewPermission || table.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
        List<Row> rows = table.query().retrieveRows();
        List<Map<String, Object>> tableData = convertRowsToMaps(rows, table, includeSystem);
        dataMap.put(tableName, tableData);
      }
    }
    allMap.put("data", dataMap);

    String output = format.mapper().writeValueAsString(allMap);
    ctx.contentType(format.contentType());
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_all_" + date + format.extension() + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void getMembers(Context ctx, OutputFormat format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema members");
    }

    TableStore store = new TableStoreForCsvInMemory();
    Emx2Members.outputRoles(store, schema);
    List<Row> rows = new ArrayList<>();
    store.readTable(Emx2Members.ROLES_TABLE).forEach(rows::add);

    List<Map<String, Object>> rowMaps = new ArrayList<>();
    for (Row row : rows) {
      rowMaps.add(row.getValueMap());
    }

    String output = format.mapper().writeValueAsString(rowMaps);
    ctx.contentType(format.contentType());
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\""
            + schema.getName()
            + "_members_"
            + date
            + format.extension()
            + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void getSettings(Context ctx, OutputFormat format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);

    TableStore store = new TableStoreForCsvInMemory();
    Emx2Settings.outputSettings(store, schema);
    List<Row> rows = new ArrayList<>();
    store.readTable(org.molgenis.emx2.Constants.SETTINGS_TABLE).forEach(rows::add);

    List<Map<String, Object>> rowMaps = new ArrayList<>();
    for (Row row : rows) {
      rowMaps.add(row.getValueMap());
    }

    String output = format.mapper().writeValueAsString(rowMaps);
    ctx.contentType(format.contentType());
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\""
            + schema.getName()
            + "_settings_"
            + date
            + format.extension()
            + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void getChangelog(Context ctx, OutputFormat format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema changelog");
    }

    int limit = parseIntParam(ctx, "limit").orElse(DEFAULT_CHANGELOG_LIMIT);
    int offset = parseIntParam(ctx, "offset").orElse(DEFAULT_CHANGELOG_OFFSET);

    TableStore store = new TableStoreForCsvInMemory();
    Emx2Changelog.outputChangelog(store, schema, limit, offset);
    List<Row> rows = new ArrayList<>();
    store.readTable(org.molgenis.emx2.Constants.CHANGELOG_TABLE).forEach(rows::add);

    List<Map<String, Object>> rowMaps = new ArrayList<>();
    for (Row row : rows) {
      rowMaps.add(row.getValueMap());
    }

    String output = format.mapper().writeValueAsString(rowMaps);
    ctx.contentType(format.contentType());
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\""
            + schema.getName()
            + "_changelog_"
            + date
            + format.extension()
            + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void getTable(Context ctx, OutputFormat format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String json = table.query().retrieveJSON();
    Object data = JSON_MAPPER.readValue(json, Object.class);
    String output = format.mapper().writeValueAsString(data);
    ctx.status(200);
    ctx.contentType(format.contentType());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + table.getName() + format.extension() + "\"");
    ctx.result(output);
  }

  private static void postTable(Context ctx, OutputFormat format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    List<Map<String, Object>> rowMaps =
        format
            .mapper()
            .readValue(
                ctx.body(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
    List<Row> rows = TypeUtils.convertToRows(table.getMetadata(), rowMaps);
    long start = System.currentTimeMillis();
    int count = table.save(rows);
    ctx.status(200);
    ctx.result(
        "{ \"message\": \"imported number of rows: "
            + count
            + "\" } in "
            + (System.currentTimeMillis() - start)
            + "ms");
  }

  private static void deleteTable(Context ctx, OutputFormat format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    List<Map<String, Object>> rowMaps =
        format
            .mapper()
            .readValue(
                ctx.body(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
    List<Row> rows = TypeUtils.convertToPrimaryKeyRows(table.getMetadata(), rowMaps);
    long start = System.currentTimeMillis();
    int count = table.delete(rows);
    ctx.status(200);
    ctx.result(
        "{ \"message\": \"deleted number of rows: "
            + count
            + "\" } in "
            + (System.currentTimeMillis() - start)
            + "ms");
  }

  private static void getRow(Context ctx, OutputFormat format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String id = DownloadApiUtils.extractIdFromPath(ctx, table);

    List<String> primaryKeyNames = table.getMetadata().getPrimaryKeys();
    DownloadApiUtils.validatePrimaryKeyCount(primaryKeyNames);

    Query query = table.query();
    if (primaryKeyNames.size() == 1) {
      Column pkColumn = table.getMetadata().getColumn(primaryKeyNames.get(0));
      Object typedId = convertToColumnType(id, pkColumn);
      query.where(f(primaryKeyNames.get(0), EQUALS, typedId));
    } else {
      String[] parts = id.split("/");
      DownloadApiUtils.validateCompositeKeyParts(parts, primaryKeyNames);
      for (int i = 0; i < primaryKeyNames.size(); i++) {
        Column pkColumn = table.getMetadata().getColumn(primaryKeyNames.get(i));
        Object typedValue = convertToColumnType(parts[i], pkColumn);
        query.where(f(primaryKeyNames.get(i), EQUALS, typedValue));
      }
    }

    List<Row> results = query.retrieveRows();
    if (results.isEmpty()) {
      ctx.status(404);
      ctx.result("{ \"message\": \"Row not found\" }");
      return;
    }

    Row result = results.get(0);
    String output = format.mapper().writeValueAsString(result.getValueMap());
    ctx.status(200);
    ctx.contentType(format.contentType());
    ctx.result(output);
  }

  private static void putRow(Context ctx, OutputFormat format) throws Exception {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String id = DownloadApiUtils.extractIdFromPath(ctx, table);

    Map<String, Object> rowMap =
        format
            .mapper()
            .readValue(
                ctx.body(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

    List<String> primaryKeyNames = table.getMetadata().getPrimaryKeys();
    if (!primaryKeyNames.isEmpty()) {
      if (primaryKeyNames.size() == 1) {
        String pkName = primaryKeyNames.get(0);
        Column pkColumn = table.getMetadata().getColumn(pkName);
        Object typedId = convertToColumnType(id, pkColumn);
        rowMap.put(pkName, typedId);
      } else {
        String[] parts = id.split("/");
        DownloadApiUtils.validateCompositeKeyParts(parts, primaryKeyNames);
        for (int i = 0; i < primaryKeyNames.size(); i++) {
          String pkName = primaryKeyNames.get(i);
          Column pkColumn = table.getMetadata().getColumn(pkName);
          Object typedValue = convertToColumnType(parts[i], pkColumn);
          rowMap.put(pkName, typedValue);
        }
      }
    }

    List<Row> rows = TypeUtils.convertToRows(table.getMetadata(), List.of(rowMap));
    timedOperation(ctx, "{ \"message\": \"updated row\" }", () -> table.save(rows));
  }

  private static void deleteRow(Context ctx, OutputFormat format) throws Exception {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String tablePath = "/" + table.getName() + "/";
    int tableIndex = ctx.path().indexOf(tablePath);
    if (tableIndex == -1) {
      throw new MolgenisException("Invalid path for table " + table.getName());
    }
    String pathAfterTable = ctx.path().substring(tableIndex + tablePath.length());
    String id = pathAfterTable;

    List<String> primaryKeyNames = table.getMetadata().getPrimaryKeys();
    if (primaryKeyNames.isEmpty()) {
      throw new MolgenisException("Table has no primary key");
    }

    Row primaryKey = new Row();
    if (primaryKeyNames.size() == 1) {
      Column pkColumn = table.getMetadata().getColumn(primaryKeyNames.get(0));
      Object typedId = convertToColumnType(id, pkColumn);
      primaryKey.set(primaryKeyNames.get(0), typedId);
    } else {
      String[] parts = id.split("/");
      if (parts.length != primaryKeyNames.size()) {
        throw new MolgenisException(
            "Composite primary key requires "
                + primaryKeyNames.size()
                + " values separated by /, got "
                + parts.length);
      }
      for (int i = 0; i < primaryKeyNames.size(); i++) {
        Column pkColumn = table.getMetadata().getColumn(primaryKeyNames.get(i));
        Object typedValue = convertToColumnType(parts[i], pkColumn);
        primaryKey.set(primaryKeyNames.get(i), typedValue);
      }
    }

    timedOperation(
        ctx, "{ \"message\": \"deleted row\" }", () -> table.delete(List.of(primaryKey)));
  }

  private static List<Map<String, Object>> convertRowsToMaps(
      List<Row> rows, Table table, boolean includeSystem) {
    Set<String> allowedColumns =
        table.getMetadata().getDownloadColumnNames().stream()
            .map(Column::getName)
            .filter(name -> name.equals(MG_DRAFT) || !name.startsWith("mg_") || includeSystem)
            .collect(java.util.stream.Collectors.toSet());

    List<Map<String, Object>> result = new ArrayList<>();
    for (Row row : rows) {
      Map<String, Object> valueMap = row.getValueMap();
      Map<String, Object> filteredRow = new LinkedHashMap<>();
      for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
        if (allowedColumns.contains(entry.getKey()) && entry.getValue() != null) {
          filteredRow.put(entry.getKey(), entry.getValue());
        }
      }
      result.add(filteredRow);
    }
    return result;
  }

  private static Object convertToColumnType(String value, Column column) {
    if (value == null) {
      return null;
    }
    ColumnType type = column.getColumnType();
    return switch (type) {
      case UUID -> TypeUtils.toUuid(value);
      case INT -> TypeUtils.toInt(value);
      case LONG -> TypeUtils.toLong(value);
      case DECIMAL -> TypeUtils.toDecimal(value);
      case BOOL -> TypeUtils.toBool(value);
      case DATE -> TypeUtils.toDate(value);
      case DATETIME -> TypeUtils.toDateTime(value);
      default -> value;
    };
  }
}
