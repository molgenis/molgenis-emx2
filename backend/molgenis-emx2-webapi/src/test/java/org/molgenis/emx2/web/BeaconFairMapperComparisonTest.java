package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import io.javalin.http.Context;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.PipelineExecutor;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * Comparison test between BeaconApi (Java) and FAIRmapper (YAML+JSLT). Verifies both produce
 * structurally compatible Beacon v2 responses.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BeaconFairMapperComparisonTest {

  private static Database database;
  private static Schema schema;
  private static final String SCHEMA_NAME = "BeaconComparisonTest";
  private static final ObjectMapper mapper = new ObjectMapper();
  private static Path bundlePath;
  private static MappingBundle bundle;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    DataModels.Profile.PATIENT_REGISTRY.getImportTask(database, SCHEMA_NAME, "test", true).run();
    schema = database.getSchema(SCHEMA_NAME);

    // Load FAIRmapper bundle
    bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/beacon-v2").normalize();
    Path configPath = bundlePath.resolve("fairmapper.yaml");
    if (Files.exists(configPath)) {
      bundle = new BundleLoader().load(configPath);
    }
  }

  @AfterAll
  static void cleanup() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  @Order(1)
  void testBothProduceValidBeaconResponse_EmptyQuery() throws Exception {
    if (bundle == null) {
      System.out.println("Skipping - bundle not found");
      return;
    }

    String requestJson =
        """
        {
          "query": {
            "pagination": { "limit": 5, "skip": 0 }
          }
        }
        """;

    // Run BeaconApi
    JsonNode beaconResult = runBeaconApi(requestJson);

    // Run FAIRmapper
    JsonNode fairmapperResult = runFairMapper(requestJson, "individuals-minimal");

    // Both should have required Beacon v2 structure
    assertBeaconStructure(beaconResult, "BeaconApi");
    assertBeaconStructure(fairmapperResult, "FAIRmapper");

    // Both should have results
    assertTrue(beaconResult.get("responseSummary").get("exists").asBoolean(), "BeaconApi: exists");
    assertTrue(
        fairmapperResult.get("responseSummary").get("exists").asBoolean(), "FAIRmapper: exists");

    // Log for comparison
    System.out.println("=== Comparison: Empty Query ===");
    System.out.println(
        "BeaconApi results: " + beaconResult.get("responseSummary").get("numTotalResults"));
    System.out.println(
        "FAIRmapper results: " + fairmapperResult.get("responseSummary").get("numTotalResults"));
  }

  @Test
  @Order(2)
  void testBothProduceValidBeaconResponse_WithSexFilter() throws Exception {
    if (bundle == null) {
      System.out.println("Skipping - bundle not found");
      return;
    }

    // Filter for males using VP concept mapping
    String requestJson =
        """
        {
          "query": {
            "filters": [
              {
                "id": "ncit:C28421",
                "value": "ncit:C16576",
                "operator": "="
              }
            ],
            "pagination": { "limit": 10, "skip": 0 }
          }
        }
        """;

    // Run BeaconApi (VP mode for concept mapping)
    JsonNode beaconResult = runBeaconApiVp(requestJson);

    // Run FAIRmapper - note: FAIRmapper has its own sex value mapping
    JsonNode fairmapperResult = runFairMapper(requestJson, "individuals-minimal");

    // Both should have valid structure
    assertBeaconStructure(beaconResult, "BeaconApi");
    assertBeaconStructure(fairmapperResult, "FAIRmapper");

    System.out.println("=== Comparison: Sex Filter ===");
    System.out.println(
        "BeaconApi results: " + beaconResult.get("responseSummary").get("numTotalResults"));
    System.out.println(1
        "FAIRmapper results: " + fairmapperResult.get("responseSummary").get("numTotalResults"));

    // Both should filter similarly (may differ due to value mapping differences)
    assertTrue(
        beaconResult.get("responseSummary").get("numTotalResults").asInt() > 0,
        "BeaconApi should have results");
    assertTrue(
        fairmapperResult.get("responseSummary").get("numTotalResults").asInt() > 0,
        "FAIRmapper should have results");
  }

  @Test
  @Order(3)
  void testResponseStructureComparison() throws Exception {
    if (bundle == null) {
      System.out.println("Skipping - bundle not found");
      return;
    }

    String requestJson =
        """
        {
          "query": {
            "pagination": { "limit": 1, "skip": 0 }
          }
        }
        """;

    JsonNode beaconResult = runBeaconApi(requestJson);
    JsonNode fairmapperResult = runFairMapper(requestJson, "individuals-minimal");

    // Compare response structure
    JsonNode beaconIndividual =
        beaconResult.get("response").get("resultSets").get(0).get("results").get(0);
    JsonNode fairmapperIndividual =
        fairmapperResult.get("response").get("resultSets").get(0).get("results").get(0);

    System.out.println("=== Individual Response Comparison ===");
    System.out.println("BeaconApi individual fields: " + getFieldNames(beaconIndividual));
    System.out.println("FAIRmapper individual fields: " + getFieldNames(fairmapperIndividual));

    // Both should have id
    assertNotNull(beaconIndividual.get("id"), "BeaconApi: individual.id");
    assertNotNull(fairmapperIndividual.get("id"), "FAIRmapper: individual.id");

    // Both should have sex
    assertNotNull(beaconIndividual.get("sex"), "BeaconApi: individual.sex");
    assertNotNull(fairmapperIndividual.get("sex"), "FAIRmapper: individual.sex");
  }

  @Test
  @Order(4)
  void writeComparisonReport() throws Exception {
    if (bundle == null) return;

    String requestJson =
        """
        {
          "query": {
            "pagination": { "limit": 3, "skip": 0 }
          }
        }
        """;

    JsonNode beaconResult = runBeaconApi(requestJson);
    JsonNode fairmapperResult = runFairMapper(requestJson, "individuals-minimal");

    StringBuilder report = new StringBuilder();
    report.append("# Beacon vs FAIRmapper Comparison Report\n\n");

    // Response summary
    report.append("## Response Summary\n");
    report.append("| Field | BeaconApi | FAIRmapper |\n");
    report.append("|-------|-----------|------------|\n");
    report
        .append("| numTotalResults | ")
        .append(beaconResult.get("responseSummary").get("numTotalResults"))
        .append(" | ")
        .append(fairmapperResult.get("responseSummary").get("numTotalResults"))
        .append(" |\n");
    report
        .append("| exists | ")
        .append(beaconResult.get("responseSummary").get("exists"))
        .append(" | ")
        .append(fairmapperResult.get("responseSummary").get("exists"))
        .append(" |\n\n");

    // Individual fields
    JsonNode beaconIndividual =
        beaconResult.get("response").get("resultSets").get(0).get("results").get(0);
    JsonNode fairmapperIndividual =
        fairmapperResult.get("response").get("resultSets").get(0).get("results").get(0);

    report.append("## Individual Fields\n");
    report.append("**BeaconApi**: ").append(getFieldNames(beaconIndividual)).append("\n\n");
    report.append("**FAIRmapper**: ").append(getFieldNames(fairmapperIndividual)).append("\n\n");

    // Full JSON samples
    report.append("## Sample Individual (BeaconApi)\n```json\n");
    report.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(beaconIndividual));
    report.append("\n```\n\n");

    report.append("## Sample Individual (FAIRmapper)\n```json\n");
    report.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fairmapperIndividual));
    report.append("\n```\n");

    // Write to file
    Path reportPath = Paths.get("build/beacon-comparison-report.md");
    Files.writeString(reportPath, report.toString());
    System.out.println("Report written to: " + reportPath.toAbsolutePath());
  }

  private JsonNode runBeaconApi(String requestJson) throws Exception {
    Context ctx = mockBeaconContext("beacon");
    BeaconRequestBody request = mapper.readValue(requestJson, BeaconRequestBody.class);
    request.addRequestParameters(ctx);

    QueryEntryType query = new QueryEntryType(request);
    return query.query(schema);
  }

  private JsonNode runBeaconApiVp(String requestJson) throws Exception {
    Context ctx = mockBeaconContext("beacon_vp");
    BeaconRequestBody request = mapper.readValue(requestJson, BeaconRequestBody.class);
    request.addRequestParameters(ctx);

    QueryEntryType query = new QueryEntryType(request);
    return query.query(schema);
  }

  private JsonNode runFairMapper(String requestJson, String endpointName) throws Exception {
    Endpoint endpoint =
        bundle.endpoints().stream()
            .filter(e -> e.path().contains(endpointName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Endpoint not found: " + endpointName));

    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(schema);
    JsltTransformEngine transformEngine = new JsltTransformEngine();
    PipelineExecutor executor = new PipelineExecutor(graphQL, transformEngine, bundlePath);

    JsonNode requestBody = mapper.readTree(requestJson);
    return executor.execute(requestBody, endpoint);
  }

  private Context mockBeaconContext(String specification) {
    Context ctx = mock(Context.class);
    when(ctx.url()).thenReturn("http://localhost:8080/" + SCHEMA_NAME + "/api/" + specification);
    when(ctx.attribute("specification")).thenReturn(specification);
    when(ctx.pathParamMap())
        .thenReturn(Map.of("entry_type", EntryType.INDIVIDUALS.getId(), "schema", SCHEMA_NAME));
    when(ctx.queryParamMap()).thenReturn(new HashMap<>());
    return ctx;
  }

  private void assertBeaconStructure(JsonNode response, String source) {
    assertNotNull(response.get("meta"), source + ": meta required");
    assertNotNull(response.get("responseSummary"), source + ": responseSummary required");
    assertNotNull(response.get("response"), source + ": response required");

    JsonNode resultSets = response.get("response").get("resultSets");
    assertNotNull(resultSets, source + ": response.resultSets required");
    assertTrue(resultSets.isArray(), source + ": resultSets should be array");
  }

  private String getFieldNames(JsonNode node) {
    StringBuilder sb = new StringBuilder("[");
    node.fieldNames().forEachRemaining(f -> sb.append(f).append(", "));
    if (sb.length() > 1) sb.setLength(sb.length() - 2);
    sb.append("]");
    return sb.toString();
  }
}
