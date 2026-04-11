package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VariantDef(
    String name,
    List<String> extendNames,
    String description,
    Boolean internal,
    List<String> profiles) {

  @JsonCreator
  public VariantDef(
      @JsonProperty("name") String name,
      @JsonProperty("extends") List<String> extendNames,
      @JsonProperty("description") String description,
      @JsonProperty("internal") Boolean internal,
      @JsonProperty("profiles") List<String> profiles) {
    this.name = name;
    this.extendNames = extendNames != null ? extendNames : List.of();
    this.description = description;
    this.internal = internal;
    this.profiles = profiles != null ? profiles : List.of();
  }

  public boolean isInternal() {
    return Boolean.TRUE.equals(internal);
  }
}
