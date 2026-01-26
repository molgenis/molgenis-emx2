package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import graphql.GraphQL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.E2eTestRunner.E2eTestResult;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class E2eTest {
  private static Database database;
  private static Path bundlePath;
  private static BundleLoader bundleLoader;
  private static JsltTransformEngine transformEngine;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/beacon-v2").normalize();
    bundleLoader = new BundleLoader();
    transformEngine = new JsltTransformEngine();
  }

  @Test
  void testBeaconE2eTests() throws Exception {
    Path configPath = bundlePath.resolve("fairmapper.yaml");
    MappingBundle bundle = bundleLoader.load(configPath);

    for (Endpoint endpoint : bundle.endpoints()) {
      if (endpoint.e2e() != null) {
        runE2eTests(endpoint);
      }
    }
  }

  private void runE2eTests(Endpoint endpoint) {
    String schemaName = endpoint.e2e().schema();
    Schema schema = database.getSchema(schemaName);

    assumeTrue(schema != null, schemaName + " schema not loaded - skipping e2e tests");

    GraphQL graphql =
        new GraphqlApiFactory().createGraphqlForSchema(schema, new TaskServiceInMemory());
    E2eTestRunner runner = new E2eTestRunner(bundlePath, endpoint, graphql, transformEngine);

    List<E2eTestResult> results = runner.runTests();

    assertFalse(results.isEmpty(), "No e2e tests found");

    for (E2eTestResult result : results) {
      assertTrue(
          result.passed(),
          String.format(
              "E2e test failed for %s %s: %s", result.method(), result.input(), result.message()));
    }
  }
}
