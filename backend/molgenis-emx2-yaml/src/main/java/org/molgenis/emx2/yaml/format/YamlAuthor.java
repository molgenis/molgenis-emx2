package org.molgenis.emx2.yaml.format;

import java.util.List;
import lombok.Builder;

@Builder
public record YamlAuthor(
    String firstName, String lastName, String email, String orcid, List<YamlAuthorRole> role) {}
