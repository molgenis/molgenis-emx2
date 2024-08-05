package org.molgenis.emx2.utils;

import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.Map;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class JavaScriptBindings {

  private Schema schema;

  public JavaScriptBindings(Schema schema) {
    this.schema = schema;
  }

  @FunctionalInterface
  public interface SimplePostClient {
    Object execute(String query, Map<String, Object> variables, String schemaId);
  }

  public SimplePostClient simplePostClient =
      (query, variables, schemaId) -> {
        GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(schema);
        return graphQL
            .execute(ExecutionInput.newExecutionInput(query).variables(variables))
            .getData();
      };
}
