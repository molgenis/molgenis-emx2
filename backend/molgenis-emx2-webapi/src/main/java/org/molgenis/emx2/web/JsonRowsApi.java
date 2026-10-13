package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.SCHEMA;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.getTableByIdOrName;
import static org.molgenis.emx2.web.MolgenisWebservice.sanitize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import graphql.ExecutionResult;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionEvaluator;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.graphql.GraphqlTableFragmentGenerator;
import org.molgenis.emx2.json.JsonUtil;

public class JsonRowsApi {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader STRICT_READER =
      MAPPER.readerFor(Object.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  private static final String STRICT = "strict";
  private static final String QUERY = "query";
  private static final String FILTER = "filter";
  private static final String ORDERBY = "orderby";
  private static final String SEARCH = "search";
  private static final String LIMIT = "limit";
  private static final String OFFSET = "offset";
  private static final String LINK = "Link";
  private static final int DEFAULT_PAGE = 1000;

  private JsonRowsApi() {}

  public static void create(Javalin app) {
    final String path = "/{schema}/api/json";
    app.get(path + "/_schema", JsonYamlApi::getSchemaJSON);
    app.post(path + "/_schema", JsonYamlApi::postSchemaJSON);
    app.delete(path + "/_schema", JsonYamlApi::deleteSchemaJSON);

    app.get(path + "/_data", JsonRowsApi::readData);
    app.post(path + "/_data", data("insert"));
    app.put(path + "/_data", data("save"));
    app.patch(path + "/_data", data("update"));
    app.delete(path + "/_data", data("delete"));

    app.get(path + "/{table}", JsonRowsApi::readTable);
    app.post(path + "/{table}", table("insert"));
    app.put(path + "/{table}", table("save"));
    app.patch(path + "/{table}", table("update"));
    app.delete(path + "/{table}", table("delete"));
  }

  private static ExecutionResult execute(Context ctx, String query, Map<String, Object> variables) {
    return execute(ctx, query, variables, false);
  }

  private static ExecutionResult execute(
      Context ctx, String query, Map<String, Object> variables, boolean readOnly) {
    GraphqlExecutor executor =
        ApplicationCachePerUser.getInstance()
            .getSchemaGraphqlForUser(sanitize(ctx.pathParam(SCHEMA)), ctx);
    MolgenisSessionHandler session = new MolgenisSessionHandler(ctx.req());
    return readOnly
        ? executor.executeReadOnly(query, variables, session)
        : executor.execute(query, variables, session);
  }

  private static void readTable(Context ctx) throws JsonProcessingException {
    Table table = getTableByIdOrName(ctx);
    String id = table.getMetadata().getIdentifier();
    String selection =
        ctx.queryParam(QUERY) != null
            ? ctx.queryParam(QUERY)
            : GraphqlTableFragmentGenerator.rowSelection(getSchema(ctx), table.getMetadata());
    int pageSize = intParam(ctx, LIMIT, DEFAULT_PAGE);
    int offset = intParam(ctx, OFFSET, 0);
    Map<String, Object> variables = new LinkedHashMap<>();
    putIfPresent(variables, FILTER, jsonParam(ctx, FILTER));
    Object orderby = jsonParam(ctx, ORDERBY);
    variables.put(ORDERBY, orderby != null ? orderby : keyOrder(table.getMetadata()));
    putIfPresent(variables, SEARCH, ctx.queryParam(SEARCH));
    variables.put(LIMIT, pageSize + 1);
    variables.put(OFFSET, offset);
    String query =
        ("query($filter:%1$sFilter,$orderby:[%1$sorderby],$search:String,$limit:Int,$offset:Int)"
                + "{%1$s(filter:$filter,orderby:$orderby,search:$search,limit:$limit,offset:$offset)%2$s}")
            .formatted(id, selection);
    List<?> rows = rowsOf(execute(ctx, query, variables, true), id);
    if (rows.size() > pageSize) {
      rows = rows.subList(0, pageSize);
      Map<String, List<String>> parameters = new LinkedHashMap<>(ctx.queryParamMap());
      parameters.put(OFFSET, List.of(String.valueOf(offset + pageSize)));
      ctx.header(LINK, nextLink(tablePath(table.getSchema().getName(), id), parameters));
    }
    respond(ctx, rows);
  }

  private static List<?> rowsOf(ExecutionResult result, String id) {
    List<?> rows = (List<?>) ((Map<?, ?>) result.getData()).get(id);
    return rows != null ? rows : List.of();
  }

  private static List<Map<String, String>> keyOrder(TableMetadata table) {
    return table.getPrimaryKeyColumns().stream()
        .map(column -> Map.of(column.getIdentifier(), "ASC"))
        .toList();
  }

  private static String keyOrderText(TableMetadata table) {
    return keyOrder(table).stream()
        .map(entry -> "{%s:ASC}".formatted(entry.keySet().iterator().next()))
        .collect(Collectors.joining(",", "[", "]"));
  }

  private static void putIfPresent(Map<String, Object> variables, String name, Object value) {
    if (value != null) {
      variables.put(name, value);
    }
  }

  private static void respond(Context ctx, Object body) throws JsonProcessingException {
    ctx.contentType(ContentType.APPLICATION_JSON)
        .result(JsonUtil.getWriter().writeValueAsString(body));
  }

  private static void readData(Context ctx) throws JsonProcessingException {
    if (ctx.queryParam(QUERY) != null) {
      respond(ctx, execute(ctx, ctx.queryParam(QUERY), null, true).getData());
      return;
    }
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema " + ctx.pathParam(SCHEMA) + " unknown");
    }
    StringBuilder query = new StringBuilder("{");
    List<String> ids = new ArrayList<>();
    for (TableMetadata table : schema.getMetadata().getTables()) {
      if (!table.getColumns().isEmpty() && PermissionEvaluator.canView(schema, table)) {
        ids.add(table.getIdentifier());
        query
            .append(
                "%s(limit:%d,orderby:%s)"
                    .formatted(table.getIdentifier(), DEFAULT_PAGE + 1, keyOrderText(table)))
            .append(GraphqlTableFragmentGenerator.rowSelection(schema, table));
      }
    }
    if (ids.isEmpty()) {
      respond(ctx, Map.of());
      return;
    }
    ExecutionResult result = execute(ctx, query.append("}").toString(), null, true);
    Map<String, Object> body = new LinkedHashMap<>();
    for (String id : ids) {
      List<?> rows = rowsOf(result, id);
      if (rows.size() > DEFAULT_PAGE) {
        rows = rows.subList(0, DEFAULT_PAGE);
        ctx.res()
            .addHeader(
                LINK,
                nextLink(
                    tablePath(schema.getName(), id), Map.of(OFFSET, List.of("" + DEFAULT_PAGE))));
      }
      body.put(id, rows);
    }
    respond(ctx, body);
  }

  private static String nextLink(String url, Map<String, List<String>> parameters) {
    String queryString =
        parameters.entrySet().stream()
            .flatMap(
                entry ->
                    entry.getValue().stream()
                        .map(value -> encode(entry.getKey()) + "=" + encode(value)))
            .collect(Collectors.joining("&"));
    return "<%s?%s>; rel=\"next\"".formatted(url, queryString);
  }

  private static String tablePath(String schemaName, String tableId) {
    return "/%s/api/json/%s".formatted(encode(schemaName).replace("+", "%20"), tableId);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static int intParam(Context ctx, String name, int defaultValue) {
    String value = ctx.queryParam(name);
    if (value == null) {
      return defaultValue;
    }
    try {
      int number = Integer.parseInt(value);
      if (number < 0 || (LIMIT.equals(name) && number == 0)) {
        throw new MolgenisException("Parameter " + name + " must be a positive number");
      }
      return number;
    } catch (NumberFormatException e) {
      throw new MolgenisException(
          "Parameter " + name + " must be a whole number of at most " + Integer.MAX_VALUE);
    }
  }

  private static Object jsonParam(Context ctx, String name) {
    String value = ctx.queryParam(name);
    if (value == null) {
      return null;
    }
    try {
      return STRICT_READER.readValue(value);
    } catch (IOException e) {
      throw new MolgenisException("Parameter " + name + " must be JSON");
    }
  }

  private static Handler data(String operation) {
    return ctx -> {
      Map<String, Object> rowsByTable = new LinkedHashMap<>();
      if (!(readBody(ctx) instanceof Map<?, ?> body)) {
        throw new MolgenisException("Body must be a JSON object of arrays of rows, by table id");
      }
      for (Map.Entry<?, ?> entry : body.entrySet()) {
        Table table = getTableByIdOrName(ctx, String.valueOf(entry.getKey()));
        String identifier = table.getMetadata().getIdentifier();
        if (rowsByTable.containsKey(identifier)) {
          throw new MolgenisException("Table " + identifier + " is given more than once");
        }
        rowsByTable.put(identifier, entry.getValue());
      }
      mutate(ctx, operation, rowsByTable);
    };
  }

  private static Handler table(String operation) {
    return ctx -> {
      Table table = getTableByIdOrName(ctx);
      mutate(
          ctx,
          operation,
          Collections.singletonMap(table.getMetadata().getIdentifier(), readBody(ctx)));
    };
  }

  private static void mutate(Context ctx, String operation, Map<String, Object> rowsByTable) {
    StringBuilder declarations = new StringBuilder();
    StringBuilder arguments = new StringBuilder();
    Map<String, Object> variables = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rowsByTable.entrySet()) {
      if (!(entry.getValue() instanceof List)) {
        throw new MolgenisException("Rows of table " + entry.getKey() + " must be a JSON array");
      }
      declarations.append(",$%1$s:[%1$sInput]".formatted(entry.getKey()));
      arguments.append(",%1$s:$%1$s".formatted(entry.getKey()));
      variables.put(entry.getKey(), entry.getValue());
    }
    if ("delete".equals(operation)) {
      declarations.append(",$strict:Boolean");
      arguments.append(",strict:$strict");
      variables.put(STRICT, Boolean.parseBoolean(ctx.queryParam(STRICT)));
    }
    String query =
        arguments.isEmpty()
            ? "mutation{%s{status message}}".formatted(operation)
            : "mutation(%s){%s(%s){status message}}"
                .formatted(declarations.substring(1), operation, arguments.substring(1));
    ExecutionResult result = execute(ctx, query, variables);
    ctx.json(((Map<?, ?>) ((Map<?, ?>) result.getData()).get(operation)));
  }

  private static Object readBody(Context ctx) throws IOException {
    return MAPPER.readValue(ctx.body(), Object.class);
  }
}
