package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.SCHEMA;
import static org.molgenis.emx2.web.MolgenisWebservice.getTableByIdOrName;
import static org.molgenis.emx2.web.MolgenisWebservice.sanitize;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Table;

public class JsonRowsApi {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String STRICT = "strict";

  private JsonRowsApi() {}

  public static void create(Javalin app) {
    final String path = "/{schema}/api/json";
    app.get(path + "/_schema", JsonYamlApi::getSchemaJSON);
    app.post(path + "/_schema", JsonYamlApi::postSchemaJSON);
    app.delete(path + "/_schema", JsonYamlApi::deleteSchemaJSON);

    app.post(path + "/_data", data("insert"));
    app.put(path + "/_data", data("save"));
    app.patch(path + "/_data", data("update"));
    app.delete(path + "/_data", data("delete"));

    app.post(path + "/{table}", table("insert"));
    app.put(path + "/{table}", table("save"));
    app.patch(path + "/{table}", table("update"));
    app.delete(path + "/{table}", table("delete"));
  }

  static ExecutionResult execute(Context ctx, String query, Map<String, Object> variables) {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    return ApplicationCachePerUser.getInstance()
        .getSchemaGraphqlForUser(schemaName, ctx)
        .execute(query, variables, new MolgenisSessionHandler(ctx.req()));
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
