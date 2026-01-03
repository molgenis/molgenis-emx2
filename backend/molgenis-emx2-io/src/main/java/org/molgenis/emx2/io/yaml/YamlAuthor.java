package org.molgenis.emx2.io.yaml;

import java.util.List;
import lombok.Builder;

@Builder
public record YamlAuthor(
    String firstName, String lastName, String email, String orcid, List<YamlAuthorRole> role) {}
