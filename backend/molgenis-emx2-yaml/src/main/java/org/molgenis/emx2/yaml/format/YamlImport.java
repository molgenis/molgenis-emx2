package org.molgenis.emx2.yaml.format;

import java.util.List;
import lombok.Builder;

@Builder
public record YamlImport(String path, List<YamlInclude> includes) {}
