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
    String model,
    List<String> demodata,
    List<String> ontologies,
    List<String> settings,
    Map<String, String> permissions) {

  @JsonCreator
  public AdditionalSchemaDef(
      @JsonProperty("model") String model,
      @JsonProperty("demodata") List<String> demodata,
      @JsonProperty("ontologies") List<String> ontologies,
      @JsonProperty("settings") List<String> settings,
      @JsonProperty("permissions") Map<String, String> permissions) {
    this.model = model;
    this.demodata = demodata != null ? demodata : List.of();
    this.ontologies = ontologies != null ? ontologies : List.of();
    this.settings = settings != null ? settings : List.of();
    this.permissions = permissions != null ? permissions : Map.of();
  }
}
