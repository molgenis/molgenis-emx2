package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.fairmapper.model.MappingBundle;

public class BundleLoader {
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public MappingBundle load(Path mappingYamlPath) throws IOException {
    if (!Files.exists(mappingYamlPath)) {
      throw new IOException("Mapping file not found: " + mappingYamlPath);
    }
    return yamlMapper.readValue(mappingYamlPath.toFile(), MappingBundle.class);
  }

  public Path resolvePath(Path bundleBasePath, String relativePath) {
    return bundleBasePath.getParent().resolve(relativePath).normalize();
  }
}
