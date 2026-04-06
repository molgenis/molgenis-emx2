package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VariantDef(List<String> inherits, String description, Boolean internal) {

  @JsonCreator
  public VariantDef(
      @JsonProperty("inherits") List<String> inherits,
      @JsonProperty("description") String description,
      @JsonProperty("internal") Boolean internal) {
    this.inherits = inherits != null ? inherits : List.of();
    this.description = description;
    this.internal = internal;
  }

  public boolean isInternal() {
    return Boolean.TRUE.equals(internal);
  }
}
