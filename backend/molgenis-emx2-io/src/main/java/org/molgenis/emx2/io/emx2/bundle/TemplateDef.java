package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateDef(String description, List<String> includes, Boolean internal) {

  @JsonCreator
  public TemplateDef(
      @JsonProperty("description") String description,
      @JsonProperty("includes") List<String> includes,
      @JsonProperty("internal") Boolean internal) {
    this.description = description;
    this.includes = includes != null ? includes : List.of();
    this.internal = internal;
  }
}
