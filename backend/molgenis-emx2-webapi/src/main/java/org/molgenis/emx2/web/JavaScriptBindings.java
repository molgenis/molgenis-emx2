package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.applicationCache;

import graphql.ExecutionInput;
import graphql.GraphQL;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class JavaScriptBindings {

  private JavaScriptBindings() {}

  private static final String SIMPLE_POST_CLIENT = "simplePostClient";

  @FunctionalInterface
  public interface SimplePostClient {
    Object execute(String query, Map<String, Object> variables, String schemaId);
  }

  private static SimplePostClient createSimplePostClient(Context ctx) {
    return (query, variables, schemaId) -> {
      GraphQL graphQL = applicationCache.getSchemaGraphqlForUser(schemaId, ctx);
      return graphQL
          .execute(ExecutionInput.newExecutionInput(query).variables(variables))
          .getData();
    };
  }

  public static Map<String, Supplier<Object>> getBindingsForContext(Context ctx) {
    Map<String, Supplier<Object>> bindings = new HashMap<>();
    bindings.put(SIMPLE_POST_CLIENT, () -> createSimplePostClient(ctx));
    // Add more bindings here in a similar way if needed

    return bindings;
  }
}
