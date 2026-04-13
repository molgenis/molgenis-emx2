package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdditionalSchemaDef(
    String bundle, List<String> data, List<String> settings, Map<String, String> permissions) {

  @JsonCreator
  public AdditionalSchemaDef(
      @JsonProperty("bundle") String bundle,
      @JsonProperty("data") List<String> data,
      @JsonProperty("settings") List<String> settings,
      @JsonProperty("permissions") Map<String, String> permissions) {
    this.bundle = bundle;
    this.data = data != null ? data : List.of();
    this.settings = settings != null ? settings : List.of();
    this.permissions = permissions != null ? permissions : Map.of();
  }
}
