package org.molgenis.emx2.io.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.molgenis.emx2.MolgenisException;

@Builder
public record YamlMolgenis(
    List<YamlTable> schema,
    Map<String, Object> settings,
    List<YamlPermission> permissions,
    YamlPackageDescription description) {

  private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  public YamlMolgenis loadFromFile(File file) throws IOException {
    try {
      return mapper.readValue(file, YamlMolgenis.class);
    } catch (IOException e) {
      throw new MolgenisException("Parsing " + file.getName() + " failed: \n" + e.getMessage());
    }
  }
}
