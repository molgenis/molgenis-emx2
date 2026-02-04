package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.engine.JsltTransformEngine;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class BeaconIntegrationTest {

  // Assumes patientRegistry schema already loaded (by DataModelsTest or similar)
  private static final String SCHEMA_NAME = "patientRegistry";
  private static Database database;
  private static Schema schema;
  private static GraphQL graphql;
  private static JsltTransformEngine transformEngine;
  private static Path bundlePath;
  private static ObjectMapper mapper = new ObjectMapper();
  private static BundleLoader bundleLoader;
  private static PipelineExecutor pipelineExecutor;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.getSchema(SCHEMA_NAME);

    // Skip tests if patientRegistry not loaded
    assumeTrue(schema != null, "patientRegistry schema not loaded - skipping");

    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, new TaskServiceInMemory());
    transformEngine = new JsltTransformEngine();
    bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/beacon-v2").normalize();
    bundleLoader = new BundleLoader();
    pipelineExecutor = new PipelineExecutor(graphql, transformEngine, bundlePath, schema);
  }

  @Test
  void testIndividualsQuery_noFilters_returns10() throws Exception {
    // Execute GraphQL query directly
    String query =
        """
        query {
          Individuals(limit: 10) {
            id
            genderAtBirth { code name }
          }
        }
        """;

    JsonNode result = executeGraphQL(query);
    JsonNode individuals = result.get("data").get("Individuals");

    assertNotNull(individuals);
    assertEquals(10, individuals.size());
  }

  @Test
  void testIndividualsQuery_withIdFilter() throws Exception {
    // Query for specific individual by ID
    String query =
        """
        query {
          Individuals(filter: {id: {equals: "Ind001"}}) {
            id
          }
        }
        """;

    JsonNode result = executeGraphQL(query);
    JsonNode individuals = result.get("data").get("Individuals");

    assertNotNull(individuals);
    assertEquals(1, individuals.size());
    assertEquals("Ind001", individuals.get(0).get("id").asText());
  }

  @Test
  @org.junit.jupiter.api.Disabled("Transform output schema changed - needs test data update")
  void testResponseTransform() throws Exception {
    // Get data via GraphQL
    String query =
        """
        query {
          Individuals(limit: 2) {
            id
            genderAtBirth { code name }
          }
        }
        """;

    JsonNode graphqlResult = executeGraphQL(query);

    // Transform to beacon format
    Path transformPath = bundlePath.resolve("src/individuals-response.jslt");
    JsonNode beaconResponse = transformEngine.transform(transformPath, graphqlResult);

    // Verify beacon structure
    assertTrue(beaconResponse.has("meta"));
    assertTrue(beaconResponse.has("responseSummary"));
    assertTrue(beaconResponse.has("response"));
    assertTrue(beaconResponse.get("responseSummary").get("exists").asBoolean());
  }

  @Test
  @org.junit.jupiter.api.Disabled("Endpoint class removed - test needs conversion to Mapping")
  void testPipelineExecutor_transformQueryTransform() throws Exception {}

  private JsonNode executeGraphQL(String query) throws Exception {
    ExecutionInput input = ExecutionInput.newExecutionInput().query(query).build();
    ExecutionResult result = graphql.execute(input);

    if (!result.getErrors().isEmpty()) {
      fail("GraphQL errors: " + result.getErrors());
    }

    String json = convertExecutionResultToJson(result);
    return mapper.readTree(json);
  }
}
