package org.molgenis.emx2.yaml.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.molgenis.emx2.MolgenisException;

@Builder
public record YamlMolgenisPackage(
    YamlPackageDescription description,
    List<YamlTable> schema,
    List<YamlPermission> permissions,
    Map<String, Object> settings,
    YamlTable table // if package only contains one table definition
    ) {

  private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  public YamlMolgenisPackage loadFromFile(File file) throws IOException {
    try {
      return mapper.readValue(file, YamlMolgenisPackage.class);
    } catch (IOException e) {
      throw new MolgenisException("Parsing " + file.getName() + " failed: \n" + e.getMessage());
    }
  }
}
