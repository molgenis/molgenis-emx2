package org.molgenis.emx2.io.yaml;

import java.util.List;
import lombok.Builder;

@Builder
public record YamlPermission(
    String role, List<String> view, List<String> create, List<String> edit, List<String> delete) {}
