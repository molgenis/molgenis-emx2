package org.molgenis.emx2.yaml.format;

import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record YamlTable(
    String table,
    String description,
    String semantics,
    YamlTableType tableType,
    List<YamlSubclass> subclasses,
    Map<String, Object> context,
    List<YamlColumn> columns,
    YamlImport imports) {}
