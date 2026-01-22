package org.molgenis.emx2.yaml.format;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;

@Builder
public record YamlSubclass(
    @NotNull String name, String description, String when, List<String> inherits) {}
