package org.molgenis.emx2.io.yaml;

import java.util.List;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@Builder
public record YamlPackageDescription(
    @NotNull String id,
    @NotNull String title,
    String version,
    YamlLicence license,
    String homepage,
    String sourceRepository,
    List<YamlAuthor> authors) {}
