package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

@Tag("slow")
public class DcatPipelineIntegrationTest {

  private static final String SCHEMA_NAME = "dcatHarvestTest";

  private static Database database;
  private static Schema schema;
  private static GraphQL graphql;
  private static Path bundlePath;
  private static ObjectMapper mapper = new ObjectMapper();

  @BeforeAll
  static void setup() throws Exception {
    database = TestDatabaseFactory.getTestDatabase();

    database.dropSchemaIfExists(SCHEMA_NAME);
    DataModels.Profile.DATA_CATALOGUE.getImportTask(database, SCHEMA_NAME, "test", true).run();
    schema = database.getSchema(SCHEMA_NAME);

    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, new TaskServiceInMemory());
    bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/dcat-fdp").normalize();
  }

  @Test
  void testTransformAndMutate_insertsResources() throws Exception {
    Path inputPath = bundlePath.resolve("test/fetch/catalog.json");
    JsonNode fetchedData = mapper.readTree(Files.readString(inputPath));

    JsltTransformEngine engine = new JsltTransformEngine();
    Path transformPath = bundlePath.resolve("src/transforms/to-molgenis.jslt");
    JsonNode transformed = engine.transform(transformPath, fetchedData);

    assertNotNull(transformed, "Transform should produce output");
    System.out.println("Transformed data: " + transformed.toPrettyString());

    String mutation =
        """
        mutation($Resources: [ResourcesInput]) {
          insert(Resources: $Resources) {
            message
          }
        }
        """;
    JsonNode mutationResult = executeGraphQL(mutation, transformed);

    assertNotNull(mutationResult, "Mutation should return result");
    assertTrue(mutationResult.has("data"), "Should have data field");
    assertNotNull(mutationResult.get("data").get("insert"), "Should have insert result");

    JsonNode resourcesResult =
        executeGraphQL(
            """
        query {
          Resources(limit: 100) {
            id
            name
            type { name }
          }
        }
        """,
            null);

    JsonNode resources = resourcesResult.get("data").get("Resources");
    assertNotNull(resources, "Should have Resources table");
    assertTrue(resources.size() > 0, "Should have inserted resources");

    System.out.println("Inserted " + resources.size() + " resources:");
    for (JsonNode r : resources) {
      System.out.println("  - " + r.get("id").asText() + ": " + r.get("name").asText());
    }
  }

  @Test
  void testTransformStep_producesValidMolgenisJson() throws Exception {
    Path inputPath = bundlePath.resolve("test/fetch/catalog.json");
    JsonNode input = mapper.readTree(Files.readString(inputPath));

    JsltTransformEngine engine = new JsltTransformEngine();
    Path transformPath = bundlePath.resolve("src/transforms/to-molgenis.jslt");
    JsonNode result = engine.transform(transformPath, input);

    assertNotNull(result);
    assertTrue(
        result.has("Resources") || result.has("Organisations"),
        "Should have Resources or Organisations");
  }

  @SuppressWarnings("unchecked")
  private JsonNode executeGraphQL(String query, JsonNode variables) throws Exception {
    ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput().query(query);

    if (variables != null) {
      inputBuilder.variables(mapper.convertValue(variables, Map.class));
    }

    ExecutionResult result = graphql.execute(inputBuilder.build());

    if (!result.getErrors().isEmpty()) {
      StringBuilder errorMsg = new StringBuilder("GraphQL errors:\n");
      for (graphql.GraphQLError error : result.getErrors()) {
        errorMsg.append("  - ").append(error.getMessage()).append("\n");
        if (error.getExtensions() != null) {
          errorMsg.append("    Extensions: ").append(error.getExtensions()).append("\n");
        }
      }
      fail(errorMsg.toString());
    }

    String json = convertExecutionResultToJson(result);
    return mapper.readTree(json);
  }
}
