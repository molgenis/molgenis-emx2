package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class FairMapperApiTest {

  private static Database database;
  private static Schema patientRegistry;
  private static final String SCHEMA_NAME = "FairMapperTestSchema";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    DataModels.Profile.PATIENT_REGISTRY.getImportTask(database, SCHEMA_NAME, "test", true).run();
    patientRegistry = database.getSchema(SCHEMA_NAME);
  }

  @AfterAll
  static void cleanup() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testBundleLoader_loadsBeaconV2() {
    Path bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/beacon-v2").normalize();
    Path configPath = bundlePath.resolve("fairmapper.yaml");

    if (!Files.exists(configPath)) {
      System.out.println("Skipping test - fairmapper.yaml not found at: " + configPath);
      return;
    }

    BundleLoader loader = new BundleLoader();
    MappingBundle bundle = loader.load(configPath);

    assertNotNull(bundle);
    assertEquals("beacon-v2", bundle.name());
    assertEquals("2.0.0", bundle.version());

    assertFalse(bundle.endpoints().isEmpty(), "Bundle should have at least one endpoint");

    var endpoint = bundle.endpoints().get(0);
    assertTrue(endpoint.path().contains("beacon"));
    assertTrue(endpoint.methods().contains("GET") || endpoint.methods().contains("POST"));
    assertFalse(endpoint.steps().isEmpty());
  }

  @Test
  void testHandleRequest_beaconIndividualsMinimal() throws Exception {
    Path bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/beacon-v2").normalize();
    Path configPath = bundlePath.resolve("fairmapper.yaml");

    if (!Files.exists(configPath)) {
      System.out.println("Skipping test - fairmapper.yaml not found at: " + configPath);
      return;
    }

    BundleLoader loader = new BundleLoader();
    MappingBundle bundle = loader.load(configPath);

    Endpoint endpoint =
        bundle.endpoints().stream()
            .filter(e -> e.path().contains("individuals-minimal"))
            .findFirst()
            .orElseThrow();

    String requestBody =
        """
        {
          "meta": {
            "requestedSchemas": [
              {
                "entityType": "Individual"
              }
            ]
          },
          "query": {
            "pagination": {
              "limit": 10,
              "skip": 0
            }
          }
        }
        """;

    Context ctx = mock(Context.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    when(ctx.pathParam("schema")).thenReturn(SCHEMA_NAME);
    when(ctx.pathParamMap()).thenReturn(Map.of("schema", SCHEMA_NAME));
    when(ctx.body()).thenReturn(requestBody);
    when(ctx.req()).thenReturn(httpRequest);
    when(httpRequest.getSession(false)).thenReturn(session);
    when(session.getAttribute("username")).thenReturn(database.getAdminUserName());

    FairMapperApi.handleRequest(ctx, endpoint, bundlePath);

    ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
    verify(ctx).contentType("application/json");
    verify(ctx).result(resultCaptor.capture());

    String response = resultCaptor.getValue();
    assertNotNull(response);

    JsonNode responseJson = objectMapper.readTree(response);
    assertTrue(responseJson.has("meta"));
    assertTrue(responseJson.has("responseSummary"));
    assertTrue(responseJson.has("response"));

    JsonNode meta = responseJson.get("meta");
    assertEquals("org.molgenis.beacon", meta.get("beaconId").asText());
    assertEquals("v2.0.0", meta.get("apiVersion").asText());

    JsonNode responseSummary = responseJson.get("responseSummary");
    assertTrue(responseSummary.has("exists"));
    assertTrue(responseSummary.has("numTotalResults"));
  }
}
