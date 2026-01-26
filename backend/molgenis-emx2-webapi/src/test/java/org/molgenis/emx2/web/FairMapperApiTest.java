package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.model.MappingBundle;

class FairMapperApiTest {

  @Test
  void testBundleLoader_loadsBeaconV2() throws IOException {
    Path bundlePath =
        Paths.get(System.getProperty("user.dir"), "../..", "fair-mappings/beacon-v2").normalize();
    Path mappingYaml = bundlePath.resolve("mapping.yaml");

    if (!Files.exists(mappingYaml)) {
      System.out.println("Skipping test - mapping.yaml not found at: " + mappingYaml);
      return;
    }

    BundleLoader loader = new BundleLoader();
    MappingBundle bundle = loader.load(mappingYaml);

    assertNotNull(bundle);
    assertEquals("molgenis.org/v1", bundle.apiVersion());
    assertEquals("FairMapperBundle", bundle.kind());
    assertEquals("beacon-v2", bundle.metadata().name());
    assertEquals("2.0.0", bundle.metadata().version());

    assertFalse(bundle.endpoints().isEmpty(), "Bundle should have at least one endpoint");

    var endpoint = bundle.endpoints().get(0);
    assertTrue(endpoint.path().contains("beacon"));
    assertTrue(endpoint.methods().contains("GET") || endpoint.methods().contains("POST"));
    assertFalse(endpoint.steps().isEmpty());
  }
}
