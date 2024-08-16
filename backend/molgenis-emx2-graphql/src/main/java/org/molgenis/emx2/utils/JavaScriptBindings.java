package org.molgenis.emx2.utils;

import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class JavaScriptBindings {

  private JavaScriptBindings() {}

  @FunctionalInterface
  public interface SimplePostClient {
    Object execute(String query, Map<String, Object> variables, String schemaId);
  }

  private static SimplePostClient createSimplePostClient(Schema schema) {
    return (query, variables, schemaId) -> {
      GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(schema);
      return graphQL
          .execute(ExecutionInput.newExecutionInput(query).variables(variables))
          .getData();
    };
  }

  public static Map<String, Supplier<Object>> getBindingsForSchema(Schema schema) {
    Map<String, Supplier<Object>> clientSuppliers = new HashMap<>();
    clientSuppliers.put("simplePostClient", () -> createSimplePostClient(schema));

    // Add more clients here in a similar way if needed

    return clientSuppliers;
  }
}
