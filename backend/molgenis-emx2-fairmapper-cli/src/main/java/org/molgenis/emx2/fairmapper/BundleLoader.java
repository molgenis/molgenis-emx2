package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.fairmapper.model.E2eTestCase;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.Step;
import org.molgenis.emx2.fairmapper.model.step.FetchStep;
import org.molgenis.emx2.fairmapper.model.step.MutateStep;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleLoader {
  private static final Logger logger = LoggerFactory.getLogger(BundleLoader.class);
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public MappingBundle load(Path mappingYamlPath) {
    validateMappingFileExists(mappingYamlPath);
    MappingBundle bundle = parseYaml(mappingYamlPath);
    validateBundle(bundle);
    validateStepFiles(mappingYamlPath, bundle);
    return bundle;
  }

  private void validateMappingFileExists(Path mappingYamlPath) {
    if (!Files.exists(mappingYamlPath)) {
      throw new FairMapperException("Mapping file not found: " + mappingYamlPath);
    }
  }

  private MappingBundle parseYaml(Path mappingYamlPath) {
    try {
      return yamlMapper.readValue(mappingYamlPath.toFile(), MappingBundle.class);
    } catch (IOException e) {
      throw new FairMapperException("Failed to parse fairmapper.yaml: " + e.getMessage(), e);
    }
  }

  private void validateBundle(MappingBundle bundle) {
    if (bundle.name() == null || bundle.name().isBlank()) {
      throw new FairMapperException("Missing required field: name");
    }
    if (bundle.version() == null || bundle.version().isBlank()) {
      logger.warn("Bundle '{}' missing version field", bundle.name());
    }
    if (bundle.getMappings().isEmpty()) {
      throw new FairMapperException(
          "Missing required field: mappings or endpoints (must have at least one)");
    }
  }

  private void validateStepFiles(Path mappingYamlPath, MappingBundle bundle) {
    Path bundleDir = mappingYamlPath.getParent();

    if (bundle.mappings() != null && !bundle.mappings().isEmpty()) {
      for (Mapping mapping : bundle.mappings()) {
        if (mapping.steps() != null && !mapping.steps().isEmpty()) {
          for (StepConfig step : mapping.steps()) {
            validateStepConfig(bundleDir, step);
          }
        }

        if (mapping.e2e() != null) {
          validateE2eTestsForMapping(bundleDir, mapping);
        }
      }
    } else if (bundle.endpoints() != null) {
      for (Endpoint endpoint : bundle.endpoints()) {
        if (endpoint.steps() != null && !endpoint.steps().isEmpty()) {
          for (Step step : endpoint.steps()) {
            validateStep(bundleDir, step);
          }
        }

        validateE2eTests(bundleDir, endpoint);
      }
    }
  }

  private void validateStep(Path bundleDir, Step step) {
    if (step.transform() != null) {
      validateTransformFile(bundleDir, step.transform());
    }
    if (step.query() != null) {
      validateQueryFile(bundleDir, step.query());
    }
    if (step.transform() == null && step.query() == null) {
      throw new FairMapperException("Step must have either transform or query defined");
    }
  }

  private void validateStepConfig(Path bundleDir, StepConfig step) {
    if (step instanceof TransformStep transformStep) {
      validateTransformFile(bundleDir, transformStep.path());
    } else if (step instanceof QueryStep queryStep) {
      validateQueryFile(bundleDir, queryStep.path());
    } else if (step instanceof MutateStep mutateStep) {
      validateQueryFile(bundleDir, mutateStep.path());
    } else if (step instanceof FetchStep fetchStep) {
      if (fetchStep.frame() != null) {
        validateFrameFile(bundleDir, fetchStep.frame());
      }
    }
  }

  private void validateFrameFile(Path bundleDir, String framePath) {
    Path fullPath = bundleDir.resolve(framePath).normalize();

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("Frame file not found: " + framePath);
    }

    if (!framePath.endsWith(".json") && !framePath.endsWith(".jsonld")) {
      throw new FairMapperException(
          "Frame file must have .json or .jsonld extension: " + framePath);
    }
  }

  private void validateTransformFile(Path bundleDir, String transformPath) {
    Path fullPath = bundleDir.resolve(transformPath).normalize();

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("Transform file not found: " + transformPath);
    }

    if (!transformPath.endsWith(".jslt")) {
      throw new FairMapperException("Transform file must have .jslt extension: " + transformPath);
    }
  }

  private void validateQueryFile(Path bundleDir, String queryPath) {
    Path fullPath = bundleDir.resolve(queryPath).normalize();

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("Query file not found: " + queryPath);
    }

    if (!queryPath.endsWith(".gql")) {
      throw new FairMapperException("Query file must have .gql extension: " + queryPath);
    }
  }

  private void validateE2eTests(Path bundleDir, Endpoint endpoint) {
    if (endpoint.e2e() == null || endpoint.e2e().tests() == null) {
      return;
    }

    for (E2eTestCase testCase : endpoint.e2e().tests()) {
      validateE2eTestFile(bundleDir, testCase.input(), "input");
      validateE2eTestFile(bundleDir, testCase.output(), "output");
    }
  }

  private void validateE2eTestsForMapping(Path bundleDir, Mapping mapping) {
    if (mapping.e2e() == null || mapping.e2e().tests() == null) {
      return;
    }

    for (E2eTestCase testCase : mapping.e2e().tests()) {
      validateE2eTestFile(bundleDir, testCase.input(), "input");
      validateE2eTestFile(bundleDir, testCase.output(), "output");
    }
  }

  private void validateE2eTestFile(Path bundleDir, String filePath, String fileType) {
    if (filePath == null || filePath.isBlank()) {
      throw new FairMapperException("E2e test " + fileType + " file path cannot be empty");
    }

    Path fullPath = bundleDir.resolve(filePath).normalize();
    if (!Files.exists(fullPath)) {
      throw new FairMapperException("E2e test " + fileType + " file not found: " + filePath);
    }
  }

  public Path resolvePath(Path bundleBasePath, String relativePath) {
    return bundleBasePath.getParent().resolve(relativePath).normalize();
  }
}
