package org.molgenis.emx2.io.yaml;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;

@Builder
public record YamlSubclass(
    @NotNull String name, String description, String when, List<String> inherits) {}
