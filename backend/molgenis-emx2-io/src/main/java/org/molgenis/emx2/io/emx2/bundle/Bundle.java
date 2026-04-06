package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Bundle(
    String name,
    String description,
    Map<String, String> namespaces,
    Map<String, TemplateDef> templates,
    Map<String, TableDef> tables) {

  @JsonCreator
  public Bundle(
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("namespaces") Map<String, String> namespaces,
      @JsonProperty("templates") Map<String, TemplateDef> templates,
      @JsonProperty("tables") Map<String, TableDef> tables) {
    this.name = name;
    this.description = description;
    this.namespaces = namespaces != null ? namespaces : Map.of();
    this.templates = templates != null ? templates : Map.of();
    this.tables = tables != null ? tables : Map.of();
  }
}
