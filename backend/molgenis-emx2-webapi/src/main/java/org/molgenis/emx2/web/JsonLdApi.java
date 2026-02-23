package org.molgenis.emx2.web;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.jsonld.JsonLdUtils.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.DownloadApiUtils.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.utils.TypeUtils;

public class JsonLdApi {

  private static final int DEFAULT_TABLE_LIMIT = 1000;
  private static final int DEFAULT_TABLE_OFFSET = 0;

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private JsonLdApi() {}

  public static void create(Javalin app) {
    for (String format : List.of("jsonld", "ttl")) {
      String api = "/{schema}/api/" + format + "-rest/";
      app.get(api + "_all", ctx -> getAll(ctx, format));
      app.get(api + "{table}", ctx -> getTable(ctx, format));
      app.get(api + "{table}/*", ctx -> getRow(ctx, format));
    }
    String jsonldApi = "/{schema}/api/jsonld-rest/";
    app.get(jsonldApi + "_schema", JsonLdApi::getSchema);
    app.get(jsonldApi + "_context", JsonLdApi::getSchema);
    app.post(jsonldApi + "{table}", ctx -> postTable(ctx, "jsonld"));
    app.delete(jsonldApi + "{table}", ctx -> deleteTable(ctx, "jsonld"));
    app.put(jsonldApi + "{table}/*", ctx -> putRow(ctx, "jsonld"));
    app.delete(jsonldApi + "{table}/*", ctx -> deleteRow(ctx, "jsonld"));
  }

  private static GraphqlExecutor getGraphqlForSchema(Context ctx) {
    String schemaName = MolgenisWebservice.sanitize(ctx.pathParam("schema"));
    return ApplicationCachePerUser.getInstance().getSchemaGraphqlForUser(schemaName, ctx);
  }

  private static String schemaBaseUrl(Context ctx) {
    String path = ctx.path();
    String schemaPath = path.substring(0, path.indexOf("/api/"));
    return ctx.scheme() + "://" + ctx.host() + schemaPath;
  }

  private static void respondWithData(
      Context ctx, String format, Map<String, Object> context, Map<String, Object> data)
      throws IOException {
    if ("ttl".equals(format)) {
      ctx.contentType(ACCEPT_TTL);
      ctx.result(convertToTurtle(context, data));
    } else {
      ctx.contentType(ACCEPT_JSONLD);
      Map<String, Object> wrapper = new LinkedHashMap<>(context);
      wrapper.put("data", data);
      ctx.result(JSON_MAPPER.writeValueAsString(wrapper));
    }
    ctx.status(200);
  }

  private static void getSchema(Context ctx) throws IOException {
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    ctx.contentType(ACCEPT_JSONLD);
    ctx.result(JSON_MAPPER.writeValueAsString(graphqlApi.getJsonLdContextMap(schemaBaseUrl(ctx))));
    ctx.status(200);
  }

  private static void getAll(Context ctx, String format) throws IOException {
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String customQuery = ctx.queryParam("query");
    String query = customQuery != null ? customQuery : graphqlApi.getSelectAllQuery();
    Map<String, Object> data = graphqlApi.queryAsMap(query, Map.of());
    respondWithData(ctx, format, graphqlApi.getJsonLdContextMap(schemaBaseUrl(ctx)), data);
  }

  private static void getTable(Context ctx, String format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String tableId = table.getMetadata().getIdentifier();
    Map<String, Object> variables = buildQueryVariables(ctx);
    String query = buildParameterizedTableQuery(tableId);
    Map<String, Object> data = graphqlApi.queryAsMap(query, variables);
    respondWithData(ctx, format, graphqlApi.getJsonLdContextMap(schemaBaseUrl(ctx)), data);
  }

  private static Map<String, Object> parsePrimaryKeyFromPath(Table table, String id) {
    List<String> primaryKeyNames = table.getMetadata().getPrimaryKeys();
    DownloadApiUtils.validatePrimaryKeyCount(primaryKeyNames);
    Map<String, Object> pkValues = new LinkedHashMap<>();
    if (primaryKeyNames.size() == 1) {
      Column pkColumn = table.getMetadata().getColumn(primaryKeyNames.get(0));
      pkValues.put(primaryKeyNames.get(0), convertToColumnType(id, pkColumn));
    } else {
      String[] parts = id.split("/");
      DownloadApiUtils.validateCompositeKeyParts(parts, primaryKeyNames);
      for (int i = 0; i < primaryKeyNames.size(); i++) {
        Column pkColumn = table.getMetadata().getColumn(primaryKeyNames.get(i));
        pkValues.put(primaryKeyNames.get(i), convertToColumnType(parts[i], pkColumn));
      }
    }
    return pkValues;
  }

  private static Filter buildPrimaryKeyFilter(Map<String, Object> pkValues) {
    List<Filter> filters =
        pkValues.entrySet().stream().map(e -> f(e.getKey(), EQUALS, e.getValue())).toList();
    return FilterBean.and(filters);
  }

  private static Optional<Row> fetchRowByPrimaryKey(Table table, String id) {
    Map<String, Object> pkValues = parsePrimaryKeyFromPath(table, id);
    List<Row> results = table.query().where(buildPrimaryKeyFilter(pkValues)).retrieveRows();
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  private static void getRow(Context ctx, String format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String id = DownloadApiUtils.extractIdFromPath(ctx, table);
    Optional<Row> result = fetchRowByPrimaryKey(table, id);
    if (result.isEmpty()) {
      ctx.status(404);
      ctx.result("{ \"message\": \"Row not found\" }");
      return;
    }
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    respondWithData(
        ctx,
        format,
        graphqlApi.getJsonLdContextMap(schemaBaseUrl(ctx)),
        result.get().getValueMap());
  }

  private static void postTable(Context ctx, String format) throws IOException {
    if ("ttl".equals(format)) {
      throw new MolgenisException("Import not supported for Turtle format");
    }
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    long start = System.currentTimeMillis();
    String body = ctx.body();
    List<Map<String, Object>> rowMaps = parseBodyToRowList(body, table);
    List<Map<String, Object>> cleaned = new ArrayList<>();
    for (Map<String, Object> rowMap : rowMaps) {
      cleaned.add(stripJsonLdKeywords(rowMap));
    }
    List<Row> rows = TypeUtils.convertToRows(table.getMetadata(), cleaned);
    int count = table.save(rows);
    long elapsed = System.currentTimeMillis() - start;
    ctx.contentType("application/json");
    ctx.result(
        JSON_MAPPER.writeValueAsString(
            Map.of("message", "imported " + count + " rows in " + elapsed + "ms")));
    ctx.status(200);
  }

  private static void deleteTable(Context ctx, String format) throws IOException {
    if ("ttl".equals(format)) {
      throw new MolgenisException("Delete not supported for Turtle format");
    }
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    long start = System.currentTimeMillis();
    List<Map<String, Object>> rowMaps =
        JSON_MAPPER.readValue(ctx.body(), new TypeReference<List<Map<String, Object>>>() {});
    List<Row> rows = TypeUtils.convertToPrimaryKeyRows(table.getMetadata(), rowMaps);
    int count = table.delete(rows);
    long elapsed = System.currentTimeMillis() - start;
    ctx.contentType("application/json");
    ctx.result(
        JSON_MAPPER.writeValueAsString(
            Map.of("message", "deleted " + count + " rows in " + elapsed + "ms")));
    ctx.status(200);
  }

  private static void putRow(Context ctx, String format) throws IOException {
    if ("ttl".equals(format)) {
      throw new MolgenisException("Update not supported for Turtle format");
    }
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String id = DownloadApiUtils.extractIdFromPath(ctx, table);
    long start = System.currentTimeMillis();
    Map<String, Object> bodyMap =
        JSON_MAPPER.readValue(ctx.body(), new TypeReference<Map<String, Object>>() {});
    Map<String, Object> cleaned = stripJsonLdKeywords(bodyMap);
    Map<String, Object> rowMap = new LinkedHashMap<>(cleaned);
    parsePrimaryKeyFromPath(table, id).forEach(rowMap::put);
    List<Row> rows = TypeUtils.convertToRows(table.getMetadata(), List.of(rowMap));
    table.save(rows);
    long elapsed = System.currentTimeMillis() - start;
    ctx.contentType("application/json");
    ctx.result(
        JSON_MAPPER.writeValueAsString(Map.of("message", "updated row in " + elapsed + "ms")));
    ctx.status(200);
  }

  private static void deleteRow(Context ctx, String format) throws IOException {
    if ("ttl".equals(format)) {
      throw new MolgenisException("Delete not supported for Turtle format");
    }
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String id = DownloadApiUtils.extractIdFromPath(ctx, table);
    long start = System.currentTimeMillis();
    Map<String, Object> pkMap = parsePrimaryKeyFromPath(table, id);
    List<Row> rows = TypeUtils.convertToPrimaryKeyRows(table.getMetadata(), List.of(pkMap));
    table.delete(rows);
    long elapsed = System.currentTimeMillis() - start;
    ctx.contentType("application/json");
    ctx.result(
        JSON_MAPPER.writeValueAsString(Map.of("message", "deleted row in " + elapsed + "ms")));
    ctx.status(200);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> parseBodyToRowList(String body, Table table)
      throws IOException {
    Object parsed = JSON_MAPPER.readValue(body, Object.class);
    if (parsed instanceof List) {
      return (List<Map<String, Object>>) parsed;
    }
    if (parsed instanceof Map) {
      Map<String, Object> bodyMap = (Map<String, Object>) parsed;
      String tableId = table.getMetadata().getIdentifier();
      for (String key : List.of(tableId, "data", "@graph")) {
        if (bodyMap.containsKey(key)) {
          Object value = bodyMap.get(key);
          if (value instanceof List) {
            return (List<Map<String, Object>>) value;
          }
          if (value instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) value;
            if (nested.containsKey(tableId)) {
              Object tableData = nested.get(tableId);
              if (tableData instanceof List) {
                return (List<Map<String, Object>>) tableData;
              }
            }
            return List.of(nested);
          }
        }
      }
      return List.of(bodyMap);
    }
    return List.of();
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

  private static String buildParameterizedTableQuery(String tableId) {
    return String.format(
        "query($limit:Int,$offset:Int,$search:String){%s(limit:$limit,offset:$offset,search:$search){...%sAllFields}}",
        tableId, tableId);
  }
}
