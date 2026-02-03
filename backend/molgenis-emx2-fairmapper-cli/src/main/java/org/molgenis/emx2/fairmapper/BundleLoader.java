package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.fairmapper.model.E2eTestCase;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.step.FrameStep;
import org.molgenis.emx2.fairmapper.model.step.MutateStep;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.SparqlConstructStep;
import org.molgenis.emx2.fairmapper.model.step.SqlQueryStep;
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

    for (Mapping mapping : bundle.mappings()) {
      validateMappingFormat(mapping);
      validateMappingFrame(bundleDir, mapping);
      try {
        mapping.validate();
      } catch (IllegalArgumentException e) {
        throw new FairMapperException(e.getMessage());
      }

      if (mapping.steps() != null && !mapping.steps().isEmpty()) {
        for (StepConfig step : mapping.steps()) {
          validateStepConfig(bundleDir, step);
        }
      }

      if (mapping.e2e() != null) {
        validateE2eTestsForMapping(bundleDir, mapping);
      }
    }
  }

  private void validateStepConfig(Path bundleDir, StepConfig step) {
    if (step instanceof TransformStep transformStep) {
      validateTransformFile(bundleDir, transformStep.path());
    } else if (step instanceof QueryStep queryStep) {
      validateQueryFile(bundleDir, queryStep.path());
    } else if (step instanceof MutateStep mutateStep) {
      validateQueryFile(bundleDir, mutateStep.path());
    } else if (step instanceof SqlQueryStep sqlQueryStep) {
      validateSqlFile(bundleDir, sqlQueryStep.path());
    } else if (step instanceof FrameStep frameStep) {
      validateFrameFile(bundleDir, frameStep.path());
    } else if (step instanceof SparqlConstructStep sparqlStep) {
      validateSparqlFile(bundleDir, sparqlStep.path());
    }
  }

  private void validateFrameFile(Path bundleDir, String framePath) {
    Path fullPath = PathValidator.validateWithinBase(bundleDir, framePath);

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("Frame file not found: " + framePath);
    }

    if (!framePath.endsWith(".json") && !framePath.endsWith(".jsonld")) {
      throw new FairMapperException(
          "Frame file must have .json or .jsonld extension: " + framePath);
    }
  }

  private void validateTransformFile(Path bundleDir, String transformPath) {
    Path fullPath = PathValidator.validateWithinBase(bundleDir, transformPath);

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("Transform file not found: " + transformPath);
    }

    if (!transformPath.endsWith(".jslt")) {
      throw new FairMapperException("Transform file must have .jslt extension: " + transformPath);
    }
  }

  private void validateQueryFile(Path bundleDir, String queryPath) {
    Path fullPath = PathValidator.validateWithinBase(bundleDir, queryPath);

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("Query file not found: " + queryPath);
    }

    if (!queryPath.endsWith(".gql")) {
      throw new FairMapperException("Query file must have .gql extension: " + queryPath);
    }
  }

  private void validateSqlFile(Path bundleDir, String sqlPath) {
    Path fullPath = PathValidator.validateWithinBase(bundleDir, sqlPath);

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("SQL file not found: " + sqlPath);
    }

    if (!sqlPath.endsWith(".sql")) {
      throw new FairMapperException("SQL file must have .sql extension: " + sqlPath);
    }
  }

  private void validateSparqlFile(Path bundleDir, String sparqlPath) {
    Path fullPath = PathValidator.validateWithinBase(bundleDir, sparqlPath);

    if (!Files.exists(fullPath)) {
      throw new FairMapperException("SPARQL file not found: " + sparqlPath);
    }

    if (!sparqlPath.endsWith(".sparql")) {
      throw new FairMapperException("SPARQL file must have .sparql extension: " + sparqlPath);
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

    Path fullPath = PathValidator.validateWithinBase(bundleDir, filePath);
    if (!Files.exists(fullPath)) {
      throw new FairMapperException("E2e test " + fileType + " file not found: " + filePath);
    }
  }

  public Path resolvePath(Path bundleBasePath, String relativePath) {
    return PathValidator.validateWithinBase(bundleBasePath.getParent(), relativePath);
  }

  private void validateMappingFormat(Mapping mapping) {
    if (mapping.input() != null && !isValidFormat(mapping.input())) {
      throw new FairMapperException("Invalid input format: " + mapping.input());
    }
    if (mapping.output() != null && !isValidFormat(mapping.output())) {
      throw new FairMapperException("Invalid output format: " + mapping.output());
    }
  }

  private void validateMappingFrame(Path bundleDir, Mapping mapping) {
    boolean isRdfInput = isRdfFormat(mapping.input());
    if (isRdfInput && (mapping.frame() == null || mapping.frame().isBlank())) {
      throw new FairMapperException(
          "RDF input format '" + mapping.input() + "' requires a frame file");
    }
    if (mapping.frame() != null && !mapping.frame().isBlank()) {
      validateFrameFile(bundleDir, mapping.frame());
    }
  }

  private boolean isValidFormat(String format) {
    return format != null
        && (format.equals("json")
            || format.equals("turtle")
            || format.equals("jsonld")
            || format.equals("ntriples")
            || format.equals("csv"));
  }

  private boolean isRdfFormat(String format) {
    return format != null
        && (format.equals("turtle") || format.equals("jsonld") || format.equals("ntriples"));
  }
}
