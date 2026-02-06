package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.engine.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.TestCase;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;

class BeaconBundleTest {
  private BundleLoader bundleLoader;
  private JsltTransformEngine transformEngine;
  private Path bundlePath;

  @BeforeEach
  void setUp() {
    bundleLoader = new BundleLoader();
    transformEngine = new JsltTransformEngine();
    bundlePath = Path.of("../../fair-mappings/beacon-v2");
  }

  @Test
  void testLoadBundle() throws Exception {
    Path configPath = bundlePath.resolve("fairmapper.yaml");
    MappingBundle bundle = bundleLoader.load(configPath);

    assertNotNull(bundle);
    assertEquals("beacon-v2", bundle.name());
    assertEquals("2.0.0", bundle.version());
    assertFalse(bundle.getMappings().isEmpty());
  }

  @Test
  void testTransformSteps() throws Exception {
    Path configPath = bundlePath.resolve("fairmapper.yaml");
    MappingBundle bundle = bundleLoader.load(configPath);

    for (Mapping mapping : bundle.getMappings()) {
      if (mapping.steps() == null) continue;
      for (StepConfig step : mapping.steps()) {
        if (step instanceof TransformStep transformStep) {
          testTransformStep(transformStep);
        }
      }
    }
  }

  private void testTransformStep(TransformStep step) throws Exception {
    if (step.tests() == null || step.tests().isEmpty()) {
      return;
    }

    Path transformPath = bundlePath.resolve(step.path());

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
              "Transform test failed for %s with input %s", step.path(), testCase.input()));
    }
  }
}
