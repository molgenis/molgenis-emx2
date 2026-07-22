package org.molgenis.emx2.io.emx2;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.SchemaMetadata;

public record Emx2YamlBundle(
    SchemaMetadata schema,
    int formatVersion,
    String version,
    Map<String, String> namespaces,
    Map<String, Map<String, List<String>>> previousNames) {

  public Emx2YamlBundle(SchemaMetadata schema, int formatVersion, String version) {
    this(schema, formatVersion, version, Map.of(), Map.of());
  }
}
