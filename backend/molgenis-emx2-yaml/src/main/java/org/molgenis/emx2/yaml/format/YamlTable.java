package org.molgenis.emx2.yaml.format;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record YamlTable(
    @JsonProperty("import") String imports,
    List<YamlInclude> include,
    String table,
    String description,
    String semantics,
    YamlTableType tableType,
    List<YamlSubclass> subclasses,
    Map<String, Object> context,
    List<YamlColumn> columns) {}
