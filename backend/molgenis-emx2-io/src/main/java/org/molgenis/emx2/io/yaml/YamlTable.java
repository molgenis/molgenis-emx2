package org.molgenis.emx2.io.yaml;

import java.util.List;
import java.util.Map;

public record YamlTable(
    String table,
    String description,
    String semantics,
    TableType tableType,
    List<YamlSubclass> subclasses,
    Map<String, Object> context,
    List<YamlColumn> columns) {}
