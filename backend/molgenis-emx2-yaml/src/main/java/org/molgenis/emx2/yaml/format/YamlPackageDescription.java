package org.molgenis.emx2.yaml.format;

import java.util.List;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@Builder
public record YamlPackageDescription(
    @NotNull String id,
    @NotNull String title,
    String description,
    String version,
    YamlLicence license,
    String homepage,
    String sourceRepository,
    List<YamlAuthor> authors) {}
