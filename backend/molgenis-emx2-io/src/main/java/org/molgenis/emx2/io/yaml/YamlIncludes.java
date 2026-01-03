package org.molgenis.emx2.io.yaml;

import java.util.List;
import lombok.Builder;

@Builder
public record YamlIncludes(String table, List<String> columns) {}
