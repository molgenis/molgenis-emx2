package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Parser;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FdpIntegrationTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Path BUNDLE_PATH =
      Path.of("../../fair-mappings/dcat-fdp").toAbsolutePath().normalize();
  private static String molgenisUrl;
  private static String schemaName;

  @BeforeAll
  static void setup() {
    molgenisUrl = System.getenv("MOLGENIS_URL");
    schemaName = System.getenv("MOLGENIS_SCHEMA");
    if (schemaName == null) schemaName = "catalogue";
  }

  @Test
  void testFdpRootWithRealData() throws Exception {
    assumeTrue(molgenisUrl != null, "MOLGENIS_URL not set, skipping integration test");

    String query = Files.readString(BUNDLE_PATH.resolve("src/queries/get-schema-metadata.gql"));
    JsonNode variables = mapper.createObjectNode().put("baseUrl", molgenisUrl);
    JsonNode result = executeGraphQL(query, variables);

    String jslt = Files.readString(BUNDLE_PATH.resolve("src/transforms/publish/to-fdp-root.jslt"));
    JsonNode enrichedInput = result.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) enrichedInput).put("baseUrl", molgenisUrl);
    JsonNode output = Parser.compileString(jslt).apply(enrichedInput);

    String jsonLd = mapper.writeValueAsString(output);
    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);

    assertFalse(rdf.isEmpty(), "RDF output should not be empty");
    System.out.println("FDP Root generated with " + rdf.size() + " triples");
  }

  @Test
  void testFdpCatalogWithRealData() throws Exception {
    assumeTrue(molgenisUrl != null, "MOLGENIS_URL not set, skipping integration test");

    String catalogId = System.getenv("TEST_CATALOG_ID");
    assumeTrue(catalogId != null, "TEST_CATALOG_ID not set, skipping catalog test");

    String query = Files.readString(BUNDLE_PATH.resolve("src/queries/get-catalog.gql"));
    JsonNode variables = mapper.createObjectNode().put("baseUrl", molgenisUrl).put("id", catalogId);
    JsonNode result = executeGraphQL(query, variables);

    String jslt =
        Files.readString(BUNDLE_PATH.resolve("src/transforms/publish/to-dcat-catalog.jslt"));
    JsonNode enrichedInput = result.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) enrichedInput).put("baseUrl", molgenisUrl);
    JsonNode output = Parser.compileString(jslt).apply(enrichedInput);

    String jsonLd = mapper.writeValueAsString(output);
    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);

    assertFalse(rdf.isEmpty(), "RDF output should not be empty");
    System.out.println("FDP Catalog generated with " + rdf.size() + " triples");
  }

  @Test
  void testFdpDatasetWithRealData() throws Exception {
    assumeTrue(molgenisUrl != null, "MOLGENIS_URL not set, skipping integration test");

    String datasetId = System.getenv("TEST_DATASET_ID");
    assumeTrue(datasetId != null, "TEST_DATASET_ID not set, skipping dataset test");

    String query = Files.readString(BUNDLE_PATH.resolve("src/queries/get-dataset.gql"));
    JsonNode variables = mapper.createObjectNode().put("baseUrl", molgenisUrl).put("id", datasetId);
    JsonNode result = executeGraphQL(query, variables);

    String jslt =
        Files.readString(BUNDLE_PATH.resolve("src/transforms/publish/to-dcat-dataset.jslt"));
    JsonNode enrichedInput = result.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) enrichedInput).put("baseUrl", molgenisUrl);
    JsonNode output = Parser.compileString(jslt).apply(enrichedInput);

    String jsonLd = mapper.writeValueAsString(output);
    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);

    assertFalse(rdf.isEmpty(), "RDF output should not be empty");
    assertTrue(
        output.has("dcat:keyword") || output.has("dcat:theme"),
        "Dataset should have keywords or themes");
    System.out.println("FDP Dataset generated with " + rdf.size() + " triples");
  }

  private JsonNode executeGraphQL(String query, JsonNode variables) throws Exception {
    String url = molgenisUrl + "/" + schemaName + "/graphql";
    String body =
        mapper.writeValueAsString(
            mapper.createObjectNode().put("query", query).set("variables", variables));

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    JsonNode json = mapper.readTree(response.body());
    if (json.has("errors")) {
      fail("GraphQL error: " + json.get("errors"));
    }
    return json.get("data");
  }
}
