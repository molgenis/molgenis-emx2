package org.molgenis.emx2.web;

import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class JavaScriptBindings {

  private JavaScriptBindings() {}

  @FunctionalInterface
  public interface SimplePostClient {
    Object execute(String query, Map<String, Object> variables, String schemaId);
  }

  private static SimplePostClient createSimplePostClient(MolgenisSession session) {
    return (query, variables, schemaId) -> {
      GraphQL graphQL = session.getGraphqlForSchema(schemaId);
      return graphQL
          .execute(ExecutionInput.newExecutionInput(query).variables(variables))
          .getData();
    };
  }

  public static Map<String, Supplier<Object>> getBindingsForSession(MolgenisSession session) {
    Map<String, Supplier<Object>> bindings = new HashMap<>();
    bindings.put("simplePostClient", () -> createSimplePostClient(session));
    // Add more bindings here in a similar way if needed

    return bindings;
  }
}
