package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.Step;

public class BundleLoader {
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
      throw new MolgenisException("Mapping file not found: " + mappingYamlPath);
    }
  }

  private MappingBundle parseYaml(Path mappingYamlPath) {
    try {
      return yamlMapper.readValue(mappingYamlPath.toFile(), MappingBundle.class);
    } catch (IOException e) {
      throw new MolgenisException("Failed to parse mapping.yaml: " + e.getMessage(), e);
    }
  }

  private void validateBundle(MappingBundle bundle) {
    if (bundle.apiVersion() == null || bundle.apiVersion().isBlank()) {
      throw new MolgenisException("Missing required field: apiVersion");
    }
    if (bundle.kind() == null || bundle.kind().isBlank()) {
      throw new MolgenisException("Missing required field: kind");
    }
    if (bundle.metadata() == null) {
      throw new MolgenisException("Missing required field: metadata");
    }
    if (bundle.endpoints() == null || bundle.endpoints().isEmpty()) {
      throw new MolgenisException("Missing required field: endpoints (must have at least one)");
    }
  }

  private void validateStepFiles(Path mappingYamlPath, MappingBundle bundle) {
    Path bundleDir = mappingYamlPath.getParent();

    for (Endpoint endpoint : bundle.endpoints()) {
      if (endpoint.steps() == null || endpoint.steps().isEmpty()) {
        continue;
      }

      for (Step step : endpoint.steps()) {
        validateStep(bundleDir, step);
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
      throw new MolgenisException("Step must have either transform or query defined");
    }
  }

  private void validateTransformFile(Path bundleDir, String transformPath) {
    Path fullPath = bundleDir.resolve(transformPath).normalize();

    if (!Files.exists(fullPath)) {
      throw new MolgenisException("Transform file not found: " + transformPath);
    }

    if (!transformPath.endsWith(".jslt")) {
      throw new MolgenisException("Transform file must have .jslt extension: " + transformPath);
    }
  }

  private void validateQueryFile(Path bundleDir, String queryPath) {
    Path fullPath = bundleDir.resolve(queryPath).normalize();

    if (!Files.exists(fullPath)) {
      throw new MolgenisException("Query file not found: " + queryPath);
    }

    if (!queryPath.endsWith(".gql")) {
      throw new MolgenisException("Query file must have .gql extension: " + queryPath);
    }
  }

  public Path resolvePath(Path bundleBasePath, String relativePath) {
    return bundleBasePath.getParent().resolve(relativePath).normalize();
  }
}
