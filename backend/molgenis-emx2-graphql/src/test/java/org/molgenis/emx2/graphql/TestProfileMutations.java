package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.sql.SqlSchema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestProfileMutations {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Database DB = TestDatabaseFactory.getTestDatabase();

  private static final String BUNDLE_YAML =
      "name: test_graphql_bundle\n"
          + "description: Bundle for GraphQL profile tests\n"
          + "subsets:\n"
          + "  subset_a:\n"
          + "    description: Subset A\n"
          + "  subset_b:\n"
          + "    description: Subset B\n"
          + "    includes: [subset_a]\n"
          + "templates:\n"
          + "  template_x:\n"
          + "    description: Template X\n"
          + "    includes: [subset_a]\n"
          + "tables:\n"
          + "  Animals:\n"
          + "    columns:\n"
          + "      id:\n"
          + "        type: int\n"
          + "        key: 1\n"
          + "      name:\n"
          + "        type: string\n"
          + "      weight:\n"
          + "        type: decimal\n"
          + "        subsets: [subset_a]\n";

  private SqlSchema createBundleSchema(String schemaName, Path tempDir) throws IOException {
    Path molgenisYaml = tempDir.resolve("molgenis.yaml");
    Files.writeString(molgenisYaml, BUNDLE_YAML);
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir);
    SqlSchema schema = (SqlSchema) DB.dropCreateSchema(schemaName);
    schema.migrate(bundle.getSchema());
    schema.attachBundle(bundle.toBundleContext());
    return schema;
  }

  private GraphqlExecutor executorFor(Schema schema) {
    return new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  private JsonNode execute(GraphqlExecutor graphql, String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = MAPPER.readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }

  private JsonNode findByName(JsonNode arrayNode, String name) {
    if (arrayNode == null || !arrayNode.isArray()) return null;
    for (JsonNode node : arrayNode) {
      JsonNode nameNode = node.get("name");
      if (nameNode != null && name.equals(nameNode.asText())) return node;
    }
    return null;
  }

  @Test
  void enableProfileCreatesColumns(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestEnableProfileCreatesColumns", tempDir);
    GraphqlExecutor graphql = executorFor(schema);

    JsonNode result =
        execute(graphql, "mutation{enableProfile(name:\"subset_a\"){message,status}}");
    assertEquals("SUCCESS", result.at("/enableProfile/status").asText());
    assertTrue(result.at("/enableProfile/message").asText().contains("subset_a"));

    JsonNode schemaResult = execute(graphql, "{_schema{activeProfiles}}");
    JsonNode activeProfiles = schemaResult.at("/_schema/activeProfiles");
    assertTrue(activeProfiles.isArray());
    boolean hasSubsetA = false;
    for (JsonNode entry : activeProfiles) {
      if ("subset_a".equals(entry.asText())) hasSubsetA = true;
    }
    assertTrue(hasSubsetA, "subset_a should be in activeProfiles after enabling");
  }

  @Test
  void enableProfileIdempotent(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestEnableProfileIdempotent", tempDir);
    GraphqlExecutor graphql = executorFor(schema);

    JsonNode first = execute(graphql, "mutation{enableProfile(name:\"subset_a\"){message,status}}");
    assertEquals("SUCCESS", first.at("/enableProfile/status").asText());

    JsonNode second =
        execute(graphql, "mutation{enableProfile(name:\"subset_a\"){message,status}}");
    assertEquals("SUCCESS", second.at("/enableProfile/status").asText());
  }

  @Test
  void enableProfileUnknownName(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestEnableProfileUnknownName", tempDir);
    GraphqlExecutor graphql = executorFor(schema);

    assertThrows(
        MolgenisException.class,
        () -> execute(graphql, "mutation{enableProfile(name:\"does_not_exist\"){message,status}}"));
  }

  @Test
  void disableProfileRemovesFromActive(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestDisableProfileRemovesFromActive", tempDir);
    GraphqlExecutor graphql = executorFor(schema);

    execute(graphql, "mutation{enableProfile(name:\"subset_a\"){message}}");

    JsonNode beforeDisable = execute(graphql, "{_schema{activeProfiles}}");
    boolean presentBefore = false;
    for (JsonNode entry : beforeDisable.at("/_schema/activeProfiles")) {
      if ("subset_a".equals(entry.asText())) presentBefore = true;
    }
    assertTrue(presentBefore, "subset_a should be active before disabling");

    JsonNode disableResult =
        execute(graphql, "mutation{disableProfile(name:\"subset_a\"){message,status}}");
    assertEquals("SUCCESS", disableResult.at("/disableProfile/status").asText());

    JsonNode afterDisable = execute(graphql, "{_schema{activeProfiles}}");
    boolean presentAfter = false;
    for (JsonNode entry : afterDisable.at("/_schema/activeProfiles")) {
      if ("subset_a".equals(entry.asText())) presentAfter = true;
    }
    assertFalse(presentAfter, "subset_a should not be active after disabling");
  }

  @Test
  void disableProfileIdempotent(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestDisableProfileIdempotent", tempDir);
    GraphqlExecutor graphql = executorFor(schema);

    JsonNode result =
        execute(graphql, "mutation{disableProfile(name:\"subset_a\"){message,status}}");
    assertEquals("SUCCESS", result.at("/disableProfile/status").asText());
  }

  @Test
  void schemaIntrospectionExposesAvailableProfiles(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestIntrospectAvailableProfiles", tempDir);
    schema.enableProfile("subset_a");
    GraphqlExecutor graphql = executorFor(schema);

    JsonNode result =
        execute(graphql, "{_schema{availableProfiles{name description includes active}}}");
    JsonNode availableProfiles = result.at("/_schema/availableProfiles");
    assertNotNull(availableProfiles);
    assertTrue(availableProfiles.isArray());
    assertTrue(availableProfiles.size() >= 2);

    JsonNode subsetA = findByName(availableProfiles, "subset_a");
    assertNotNull(subsetA, "subset_a should be in availableProfiles");
    assertEquals("Subset A", subsetA.get("description").asText());
    assertTrue(subsetA.get("active").asBoolean(), "subset_a should be active");

    JsonNode subsetB = findByName(availableProfiles, "subset_b");
    assertNotNull(subsetB, "subset_b should be in availableProfiles");
    assertFalse(subsetB.get("active").asBoolean(), "subset_b should not be active");
    assertTrue(subsetB.get("includes").isArray());
    assertEquals("subset_a", subsetB.get("includes").get(0).asText());
  }

  @Test
  void schemaIntrospectionExposesBundle(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestIntrospectBundleMeta", tempDir);
    GraphqlExecutor graphql = executorFor(schema);

    JsonNode result = execute(graphql, "{_schema{bundleName bundleDescription}}");
    assertEquals("test_graphql_bundle", result.at("/_schema/bundleName").asText());
    assertEquals(
        "Bundle for GraphQL profile tests", result.at("/_schema/bundleDescription").asText());
  }

  @Test
  void schemaIntrospectionNonBundleSchema() throws IOException {
    Schema schema = DB.dropCreateSchema("TestIntrospectNonBundleSchema");
    GraphqlExecutor graphql = executorFor(schema);

    JsonNode result =
        execute(
            graphql,
            "{_schema{activeProfiles availableProfiles{name} bundleName bundleDescription}}");
    assertTrue(result.at("/_schema/activeProfiles").isArray());
    assertEquals(0, result.at("/_schema/activeProfiles").size());
    assertTrue(result.at("/_schema/availableProfiles").isArray());
    assertEquals(0, result.at("/_schema/availableProfiles").size());
    JsonNode bundleName = result.at("/_schema/bundleName");
    assertTrue(bundleName.isNull() || bundleName.isMissingNode(), "bundleName should be null");
    JsonNode bundleDescription = result.at("/_schema/bundleDescription");
    assertTrue(
        bundleDescription.isNull() || bundleDescription.isMissingNode(),
        "bundleDescription should be null");
  }
}
