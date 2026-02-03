package org.molgenis.emx2.web;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.jsonld.RestOverGraphql.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.DownloadApiUtils.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.*;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.rdf.jsonld.JsonLdSchemaGenerator;
import org.molgenis.emx2.rdf.RdfDataValidationService;
import org.molgenis.emx2.rdf.RdfSchemaValidationService;
import org.molgenis.emx2.rdf.shacl.ShaclSelector;
import org.molgenis.emx2.rdf.shacl.ShaclSet;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.emx2.utils.URLUtils;

public class RestApi {

  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;
  private static final int DEFAULT_TABLE_LIMIT = 1000;
  private static final int DEFAULT_TABLE_OFFSET = 0;

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  private RestApi() {}

  private enum Format {
    JSON(ACCEPT_JSON, ".json", JSON_MAPPER),
    YAML(ACCEPT_YAML, ".yaml", YAML_MAPPER),
    JSONLD(ACCEPT_JSONLD, ".jsonld", JSON_MAPPER),
    TTL(ACCEPT_TTL, ".ttl", null);

    private final String contentType;
    private final String extension;
    private final ObjectMapper mapper;

    Format(String contentType, String extension, ObjectMapper mapper) {
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
    registerDatabaseEndpoints(app);
    registerSchemaEndpoints(app, "json", Format.JSON);
    registerSchemaEndpoints(app, "yaml", Format.YAML);
    registerSchemaEndpoints(app, "jsonld", Format.JSONLD);
    registerSchemaEndpoints(app, "ttl", Format.TTL);
    app.get("/{schema}/api/shacls", RestApi::getShaclSets);
    app.get("/{schema}/api/shacl", ctx -> validateAll(ctx, Format.TTL));
  }

  private static void registerDatabaseEndpoints(Javalin app) {
    app.get("/api/jsonld", RestApi::handleDatabaseLevelRequest);
    app.get("/api/ttl", RestApi::handleDatabaseLevelRequest);
  }

  private static void handleDatabaseLevelRequest(Context ctx) throws IOException {
    if (ctx.queryParam("shacls") != null) {
      getShaclSets(ctx);
    } else {
      ctx.status(404);
      ctx.contentType(ACCEPT_JSON);
      ctx.result("{ \"message\": \"Not found\" }");
    }
  }

  private static GraphqlExecutor getGraphqlForSchema(Context ctx) {
    String schemaName = MolgenisWebservice.sanitize(ctx.pathParam("schema"));
    return MolgenisWebservice.applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
  }

  private static void registerSchemaEndpoints(Javalin app, String path, Format format) {
    final String apiPath = "/{schema}/api/" + path + "/";

    app.get(
        apiPath + "_schema",
        ctx -> {
          if (ctx.queryParam("validate") != null
              && (format == Format.JSONLD || format == Format.TTL)) {
            validateSchema(ctx, format);
          } else {
            getSchemaMetadata(ctx, format);
          }
        });
    app.post(apiPath + "_schema", ctx -> postSchema(ctx, format));
    app.delete(apiPath + "_schema", ctx -> deleteSchema(ctx, format));
    app.get(
        apiPath + "_data",
        ctx -> {
          if (ctx.queryParam("validate") != null
              && (format == Format.JSONLD || format == Format.TTL)) {
            validateData(ctx, format);
          } else {
            getData(ctx, format);
          }
        });
    app.get(
        apiPath + "_all",
        ctx -> {
          if (ctx.queryParam("validate") != null
              && (format == Format.JSONLD || format == Format.TTL)) {
            validateAll(ctx, format);
          } else {
            getAll(ctx, format);
          }
        });
    app.get(apiPath + "_members", ctx -> getMembers(ctx, format));
    app.get(apiPath + "_settings", ctx -> getSettings(ctx, format));
    app.get(apiPath + "_changelog", ctx -> getChangelog(ctx, format));

    if (format == Format.JSONLD) {
      app.get(apiPath + "_context", ctx -> getContext(ctx));
    }

    app.get(apiPath + "{table}", ctx -> getTable(ctx, format));
    app.post(apiPath + "{table}", ctx -> postTable(ctx, format));
    app.delete(apiPath + "{table}", ctx -> deleteTable(ctx, format));
    app.get(apiPath + "{table}/*", ctx -> getRow(ctx, format));
    app.put(apiPath + "{table}/*", ctx -> putRow(ctx, format));
    app.delete(apiPath + "{table}/*", ctx -> deleteRow(ctx, format));
  }

  private static void getSchemaMetadata(Context ctx, Format format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    String output = formatSchema(schema.getMetadata(), format, ctx.url());
    ctx.contentType(format.contentType());
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_" + date + format.extension() + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void postSchema(Context ctx, Format format) throws Exception {
    if (format == Format.TTL) {
      throw new MolgenisException("Schema import not supported for Turtle format");
    }
    Schema schema = MolgenisWebservice.getSchema(ctx);
    String body = ctx.body();
    if (format == Format.JSONLD) {
      Map<String, Object> jsonLd = JSON_MAPPER.readValue(body, Map.class);
      body = JSON_MAPPER.writeValueAsString(stripJsonLdKeywords(jsonLd));
    }
    SchemaMetadata otherSchema =
        (format == Format.JSON || format == Format.JSONLD)
            ? jsonToSchema(body)
            : yamlToSchema(body);
    timedOperation(
        ctx, "{ \"message\": \"add/update metadata success\" }", () -> schema.migrate(otherSchema));
  }

  private static void deleteSchema(Context ctx, Format format) throws Exception {
    if (format == Format.TTL) {
      throw new MolgenisException("Schema delete not supported for Turtle format");
    }
    String body = ctx.body();
    if (format == Format.JSONLD) {
      Map<String, Object> jsonLd = JSON_MAPPER.readValue(body, Map.class);
      body = JSON_MAPPER.writeValueAsString(stripJsonLdKeywords(jsonLd));
    }
    SchemaMetadata schemaMetadata =
        (format == Format.JSON || format == Format.JSONLD)
            ? jsonToSchema(body)
            : yamlToSchema(body);
    timedOperation(
        ctx,
        "{ \"message\": \"removed metadata items success\" }",
        () -> MolgenisWebservice.getSchema(ctx).discard(schemaMetadata));
  }

  private static String formatSchema(SchemaMetadata meta, Format format, String schemaUrl)
      throws IOException {
    if (format == Format.TTL) {
      Map<String, Object> context =
          MolgenisWebservice.applicationCache.getJsonLdContext(meta.getName(), schemaUrl, meta);
      return convertToTurtle(context, Map.of());
    }
    if (format == Format.JSONLD) {
      return JsonLdSchemaGenerator.generateJsonLdSchema(meta, schemaUrl);
    }
    if (format == Format.YAML) {
      return schemaToYaml(meta, true);
    }
    return JsonUtil.schemaToJson(meta, true);
  }

  private static void getData(Context ctx, Format format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String customQuery = ctx.queryParam("query");

    Map<String, Object> data = queryAllData(graphqlApi, customQuery);
    String output = formatData(data, format, graphqlApi.getSchema().getMetadata(), ctx.url());

    ctx.contentType(format.contentType());
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_data_" + date + format.extension() + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static void getAll(Context ctx, Format format) throws IOException {
    Schema schema = MolgenisWebservice.getSchema(ctx);
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String customQuery = ctx.queryParam("query");

    Map<String, Object> data = queryAllData(graphqlApi, customQuery);
    String output;

    if (format == Format.JSONLD || format == Format.TTL) {
      output = formatData(data, format, schema.getMetadata(), ctx.url());
    } else {
      Map<String, Object> allMap = new LinkedHashMap<>();
      String schemaStr = formatSchema(schema.getMetadata(), format, ctx.url());
      Object schemaData = format.mapper().readValue(schemaStr, Object.class);
      allMap.put("schema", schemaData);
      allMap.put("data", data);
      output = format.mapper().writeValueAsString(allMap);
    }

    ctx.contentType(format.contentType());
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_all_" + date + format.extension() + "\"");
    ctx.status(200);
    ctx.result(output);
  }

  private static Map<String, Object> queryAllData(GraphqlExecutor graphqlApi, String customQuery) {
    String query = customQuery != null ? customQuery : graphqlApi.getSelectAllQuery();
    return graphqlApi.queryAsMap(query, Map.of());
  }

  private static String formatData(
      Map<String, Object> data, Format format, SchemaMetadata meta, String schemaUrl)
      throws IOException {
    if (format == Format.TTL) {
      Map<String, Object> context =
          MolgenisWebservice.applicationCache.getJsonLdContext(meta.getName(), schemaUrl, meta);
      return convertToTurtle(context, data);
    }
    if (format == Format.JSONLD) {
      Map<String, Object> context =
          MolgenisWebservice.applicationCache.getJsonLdContext(meta.getName(), schemaUrl, meta);
      Map<String, Object> wrapper = new LinkedHashMap<>();
      wrapper.putAll(context);
      wrapper.put("data", data);
      return JSON_MAPPER.writeValueAsString(wrapper);
    }
    return format.mapper().writeValueAsString(data);
  }

  private static void getMembers(Context ctx, Format format) throws IOException {
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
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
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

  private static void getSettings(Context ctx, Format format) throws IOException {
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
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
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

  private static void getChangelog(Context ctx, Format format) throws IOException {
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
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
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

  private static void getContext(Context ctx) {
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    ctx.header("Content-Type", ACCEPT_JSONLD);
    ctx.result(graphqlApi.getJsonLdSchema(ctx.url()));
  }

  private static void getTable(Context ctx, Format format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String tableId = table.getMetadata().getIdentifier();

    Map<String, Object> variables = buildQueryVariables(ctx);
    String query = buildTableQuery(tableId, variables);
    Map<String, Object> data = graphqlApi.queryAsMap(query, Map.of());

    String output = formatTableData(data, format, graphqlApi.getSchema().getMetadata(), ctx.url());

    ctx.status(200);
    ctx.contentType(format.contentType());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + table.getName() + format.extension() + "\"");
    ctx.result(output);
  }

  private static String formatTableData(
      Map<String, Object> data, Format format, SchemaMetadata meta, String schemaUrl)
      throws IOException {
    if (format == Format.TTL) {
      Map<String, Object> context =
          MolgenisWebservice.applicationCache.getJsonLdContext(meta.getName(), schemaUrl, meta);
      return convertToTurtle(context, data);
    }
    if (format == Format.JSONLD) {
      Map<String, Object> context =
          MolgenisWebservice.applicationCache.getJsonLdContext(meta.getName(), schemaUrl, meta);
      Map<String, Object> wrapper = new LinkedHashMap<>();
      wrapper.putAll(context);
      wrapper.put("data", data);
      return JSON_MAPPER.writeValueAsString(wrapper);
    }
    return format.mapper().writeValueAsString(data);
  }

  private static void postTable(Context ctx, Format format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);

    if (format == Format.JSONLD) {
      Map<String, Object> jsonLdData =
          JSON_MAPPER.readValue(
              ctx.body(),
              new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
      long start = System.currentTimeMillis();
      int count = importJsonLd(table, jsonLdData);
      ctx.status(200);
      ctx.result(
          "{ \"message\": \"imported number of rows: "
              + count
              + "\" } in "
              + (System.currentTimeMillis() - start)
              + "ms");
      return;
    }

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

  private static void deleteTable(Context ctx, Format format) throws IOException {
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

  private static void getRow(Context ctx, Format format) throws IOException {
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

  private static void putRow(Context ctx, Format format) throws Exception {
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

  private static void deleteRow(Context ctx, Format format) throws Exception {
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

  private static Map<String, Object> buildQueryVariables(Context ctx) {
    Map<String, Object> variables = new LinkedHashMap<>();

    String filterParam = ctx.queryParam("filter");
    if (filterParam != null) {
      try {
        Map<String, Object> filter = JSON_MAPPER.readValue(filterParam, Map.class);
        variables.put("filter", filter);
      } catch (Exception e) {
        throw new MolgenisException("Invalid filter JSON: " + e.getMessage());
      }
    }

    int limit = parseIntParam(ctx, "limit").orElse(DEFAULT_TABLE_LIMIT);
    int offset = parseIntParam(ctx, "offset").orElse(DEFAULT_TABLE_OFFSET);
    variables.put("limit", limit);
    variables.put("offset", offset);

    String searchParam = ctx.queryParam("search");
    if (searchParam != null) {
      variables.put("search", searchParam);
    }

    return variables;
  }

  private static String buildTableQuery(String tableId, Map<String, Object> variables) {
    List<String> argParts = new ArrayList<>();

    if (variables.containsKey("limit") && variables.get("limit") != null) {
      argParts.add("limit:" + variables.get("limit"));
    }
    if (variables.containsKey("offset") && variables.get("offset") != null) {
      argParts.add("offset:" + variables.get("offset"));
    }
    if (variables.containsKey("search") && variables.get("search") != null) {
      argParts.add("search:\"" + variables.get("search").toString().replace("\"", "\\\"") + "\"");
    }

    String args = argParts.isEmpty() ? "" : "(" + String.join(",", argParts) + ")";
    return String.format("{%s%s{...All%sFields}}", tableId, args, tableId);
  }

  private static void getShaclSets(Context ctx) throws IOException {
    ctx.contentType(ACCEPT_YAML);

    if (MolgenisWebservice.applicationCache.getDatabaseForUser(ctx).getSchemaNames().isEmpty()) {
      throw new MolgenisException("No permission to view any schema to use SHACLs on");
    }

    ObjectMapper mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build());

    try (OutputStream outputStream = ctx.outputStream()) {
      mapper.writeValue(outputStream, ShaclSelector.getAllFiltered());
    }
  }

  private static void validateSchema(Context ctx, Format format) throws Exception {
    String shaclId = MolgenisWebservice.sanitize(ctx.queryParam("validate"));
    ShaclSet shaclSet = ShaclSelector.get(shaclId);
    if (shaclSet == null) {
      ctx.status(404);
      ctx.contentType(ACCEPT_JSON);
      ctx.result("{ \"message\": \"Validation set not found: " + shaclId + "\" }");
      return;
    }

    Schema schema = MolgenisWebservice.getSchema(ctx);
    RDFFormat rdfFormat = format == Format.JSONLD ? RDFFormat.JSONLD : RDFFormat.TURTLE;
    String baseUrl = URLUtils.extractBaseURL(ctx);

    ctx.contentType(format.contentType());
    try (OutputStream out = ctx.outputStream()) {
      try (RdfSchemaValidationService service =
          new RdfSchemaValidationService(baseUrl, schema, rdfFormat, out, shaclSet)) {
        service.getGenerator().generate(schema);
      }
    }
  }

  private static void validateData(Context ctx, Format format) throws Exception {
    String shaclId = MolgenisWebservice.sanitize(ctx.queryParam("validate"));
    ShaclSet shaclSet = ShaclSelector.get(shaclId);
    if (shaclSet == null) {
      ctx.status(404);
      ctx.contentType(ACCEPT_JSON);
      ctx.result("{ \"message\": \"Validation set not found: " + shaclId + "\" }");
      return;
    }

    Schema schema = MolgenisWebservice.getSchema(ctx);
    RDFFormat rdfFormat = format == Format.JSONLD ? RDFFormat.JSONLD : RDFFormat.TURTLE;
    String baseUrl = URLUtils.extractBaseURL(ctx);

    ctx.contentType(format.contentType());
    try (OutputStream out = ctx.outputStream()) {
      try (RdfDataValidationService service =
          new RdfDataValidationService(baseUrl, schema, rdfFormat, out, shaclSet)) {
        service.getGenerator().generate(schema);
      }
    }
  }

  private static void validateAll(Context ctx, Format format) throws Exception {
    String shaclId = MolgenisWebservice.sanitize(ctx.queryParam("validate"));
    ShaclSet shaclSet = ShaclSelector.get(shaclId);
    if (shaclSet == null) {
      ctx.status(404);
      ctx.contentType(ACCEPT_JSON);
      ctx.result("{ \"message\": \"Validation set not found: " + shaclId + "\" }");
      return;
    }

    Schema schema = MolgenisWebservice.getSchema(ctx);
    RDFFormat rdfFormat = format == Format.JSONLD ? RDFFormat.JSONLD : RDFFormat.TURTLE;
    String baseUrl = URLUtils.extractBaseURL(ctx);

    ctx.contentType(format.contentType());
    try (OutputStream out = ctx.outputStream()) {
      try (RdfDataValidationService service =
          new RdfDataValidationService(baseUrl, schema, rdfFormat, out, shaclSet)) {
        service.getGenerator().generate(schema);
      }
    }
  }
}
