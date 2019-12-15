package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;

import java.util.List;
import java.util.Map;

public class GraphqlTest {
  private static Database database;

  @BeforeClass
  public static void setup() {
    database = DatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSchema() throws JsonProcessingException {

    Schema s = database.createSchema("GraphqlTest");

    PetStoreExample.create(s.getMetadata());
    PetStoreExample.populate(s);

    GraphQL graphQL = GraphqlApi.createGraphqlForSchema(s);

    executeQuery(graphQL, "query{Pet{name,category{name},status}}");

    executeQuery(graphQL, "query{Pet(filter:{status:{eq:[\"available\"]}}) {name,category{name}}}");

    executeQuery(
        graphQL, "query{Pet(filter:{category:{name:{eq:[\"cat\"]}}}) {name,category{name}}}");
  }

  private void executeQuery(GraphQL graphQL, String query) throws JsonProcessingException {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).build();

    ExecutionResult executionResult = graphQL.execute(executionInput);

    Object data = executionResult.getData();

    Map<String, Object> toSpecificationResult = executionResult.toSpecification();

    List<GraphQLError> errors = executionResult.getErrors();

    System.out.println(JsonApi.getWriter().writeValueAsString(toSpecificationResult));

    for (GraphQLError e : errors) {
      System.out.println(e);
    }
  }
}
