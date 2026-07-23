package org.molgenis.emx2.io.emx2;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.SchemaMetadata;

public record Emx2YamlBundle(
    SchemaMetadata schema,
    int formatVersion,
    String version,
    Map<String, String> namespaces,
    Map<String, Map<String, List<String>>> previousNames,
    ModelDrops drops,
    List<String> dataFiles,
    List<String> demoFiles) {

  public Emx2YamlBundle(SchemaMetadata schema, int formatVersion, String version) {
    this(
        schema,
        formatVersion,
        version,
        Map.of(),
        Map.of(),
        ModelDrops.empty(),
        List.of(),
        List.of());
  }

  public Emx2YamlBundle(
      SchemaMetadata schema,
      int formatVersion,
      String version,
      Map<String, String> namespaces,
      Map<String, Map<String, List<String>>> previousNames) {
    this(
        schema,
        formatVersion,
        version,
        namespaces,
        previousNames,
        ModelDrops.empty(),
        List.of(),
        List.of());
  }

  public Emx2YamlBundle(
      SchemaMetadata schema,
      int formatVersion,
      String version,
      Map<String, String> namespaces,
      Map<String, Map<String, List<String>>> previousNames,
      ModelDrops drops) {
    this(schema, formatVersion, version, namespaces, previousNames, drops, List.of(), List.of());
  }
}
