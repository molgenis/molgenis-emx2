package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.web.graphql.GraphQLFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Map;

import static spark.Spark.*;

public class GraphqlApi {

  private GraphqlApi() {
    // hide constructor
  }

  public static void create() {

    // schema level operations
    final String schemaPath = "/api/graphql/:schema"; // NOSONAR
    get(schemaPath, GraphqlApi::getQuery);
    post(schemaPath, GraphqlApi::getQuery);
  }

  private static String getQuery(Request request, Response response) throws IOException {

    // very expensive for now, somehow need to cache this I suppose
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    GraphQLSchema gl = GraphQLFactory.create(schema);

    String query = null;
    if ("POST".equals(request.requestMethod())) {
      ObjectNode node = new ObjectMapper().readValue(request.body(), ObjectNode.class);
      query = node.get("query").asText();
    } else {
      query =
          request.queryParamOrDefault(
              "query",
              "{\n"
                  + "  __schema {\n"
                  + "    types {\n"
                  + "      name\n"
                  + "    }\n"
                  + "  }\n"
                  + "}");
    }
    System.out.println("query\n" + query);
    GraphQL g = GraphQL.newGraphQL(gl).build();

    ExecutionResult executionResult = g.execute(query);
    for (GraphQLError err : executionResult.getErrors()) {
      System.err.println(err);
    }
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    String result = JsonApi.getWriter().writeValueAsString(toSpecificationResult);
    System.out.println("result:\n" + result);
    return result;
  }
}
