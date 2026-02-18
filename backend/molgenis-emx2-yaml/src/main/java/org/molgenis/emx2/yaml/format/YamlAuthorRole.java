package org.molgenis.emx2.yaml.format;

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
