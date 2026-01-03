package org.molgenis.emx2.io.yaml;

import java.util.List;

public record YamlImport(String path, List<YamlInclude> includes) {}
