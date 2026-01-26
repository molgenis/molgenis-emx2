package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.Step;
import org.molgenis.emx2.fairmapper.model.TestCase;

class BeaconBundleTest {
  private BundleLoader bundleLoader;
  private JsltTransformEngine transformEngine;
  private Path bundlePath;

  @BeforeEach
  void setUp() {
    bundleLoader = new BundleLoader();
    transformEngine = new JsltTransformEngine();
    bundlePath = Paths.get("/Users/m.a.swertz/git/molgenis-emx2/etl_pilot/fair-mappings/beacon-v2");
  }

  @Test
  void testLoadBundle() throws Exception {
    Path mappingYaml = bundlePath.resolve("mapping.yaml");
    MappingBundle bundle = bundleLoader.load(mappingYaml);

    assertNotNull(bundle);
    assertEquals("molgenis.org/v1", bundle.apiVersion());
    assertEquals("FairMapperBundle", bundle.kind());
    assertEquals("beacon-v2", bundle.metadata().name());
    assertEquals("2.0.0", bundle.metadata().version());
    assertFalse(bundle.endpoints().isEmpty());
  }

  @Test
  void testTransformSteps() throws Exception {
    Path mappingYaml = bundlePath.resolve("mapping.yaml");
    MappingBundle bundle = bundleLoader.load(mappingYaml);

    for (Endpoint endpoint : bundle.endpoints()) {
      for (Step step : endpoint.steps()) {
        if (step.transform() != null) {
          testTransformStep(step);
        }
      }
    }
  }

  private void testTransformStep(Step step) throws Exception {
    if (step.tests() == null || step.tests().isEmpty()) {
      return;
    }

    Path transformPath = bundlePath.resolve(step.transform());

    for (TestCase testCase : step.tests()) {
      Path inputPath = bundlePath.resolve(testCase.input());
      Path expectedOutputPath = bundlePath.resolve(testCase.output());

      JsonNode input = transformEngine.loadJson(inputPath);
      JsonNode expectedOutput = transformEngine.loadJson(expectedOutputPath);
      JsonNode actualOutput = transformEngine.transform(transformPath, input);

      assertEquals(
          expectedOutput,
          actualOutput,
          String.format(
              "Transform test failed for %s with input %s", step.transform(), testCase.input()));
    }
  }
}
