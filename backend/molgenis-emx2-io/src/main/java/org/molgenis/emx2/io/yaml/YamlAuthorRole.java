package org.molgenis.emx2.io.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YamlAuthorRole {
  @JsonProperty("creator")
  CREATOR,
  @JsonProperty("author")
  AUTHOR,
  @JsonProperty("contributor")
  CONTRIBUTOR,
  @JsonProperty("copyrightholder")
  COPYRIGHTHOLDER,
  @JsonProperty("funder")
  FUNDER
}
