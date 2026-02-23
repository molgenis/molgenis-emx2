package org.molgenis.emx2.web;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.jsonld.RestOverGraphql.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.DownloadApiUtils.*;

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
    Map<String, Object> context =
        ApplicationCachePerUser.getInstance()
            .getJsonLdContext(schemaBaseUrl(ctx), graphqlApi.getSchema().getMetadata());
    ctx.contentType(ACCEPT_JSONLD);
    ctx.result(JSON_MAPPER.writeValueAsString(context));
    ctx.status(200);
  }

  private static void getAll(Context ctx, String format) throws IOException {
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String customQuery = ctx.queryParam("query");
    String query = customQuery != null ? customQuery : graphqlApi.getSelectAllQuery();
    Map<String, Object> data = graphqlApi.queryAsMap(query, Map.of());
    Map<String, Object> context =
        ApplicationCachePerUser.getInstance()
            .getJsonLdContext(schemaBaseUrl(ctx), graphqlApi.getSchema().getMetadata());
    respondWithData(ctx, format, context, data);
  }

  private static void getTable(Context ctx, String format) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    GraphqlExecutor graphqlApi = getGraphqlForSchema(ctx);
    String tableId = table.getMetadata().getIdentifier();
    Map<String, Object> variables = buildQueryVariables(ctx);
    String query = buildParameterizedTableQuery(tableId);
    Map<String, Object> data = graphqlApi.queryAsMap(query, variables);
    Map<String, Object> context =
        ApplicationCachePerUser.getInstance()
            .getJsonLdContext(schemaBaseUrl(ctx), graphqlApi.getSchema().getMetadata());
    respondWithData(ctx, format, context, data);
  }

  private static Optional<Row> fetchRowByPrimaryKey(Table table, String id) {
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
    Map<String, Object> context =
        ApplicationCachePerUser.getInstance()
            .getJsonLdContext(schemaBaseUrl(ctx), table.getSchema().getMetadata());
    respondWithData(ctx, format, context, result.get().getValueMap());
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
