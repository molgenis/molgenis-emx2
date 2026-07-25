package org.molgenis.emx2.yaml.format;

import lombok.Builder;

@Builder
public record YamlInclude(String table, String[] columns) {}
