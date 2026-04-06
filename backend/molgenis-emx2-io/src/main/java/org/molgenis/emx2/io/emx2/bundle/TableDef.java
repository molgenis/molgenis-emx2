package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TableDef(
    String description,
    List<String> inherits,
    List<String> profiles,
    List<String> semantics,
    Boolean internal,
    String label,
    String oldName,
    String importSchema,
    Map<String, VariantDef> variants,
    Map<String, DataColumn> columns,
    Map<String, SectionDef> sections) {

  @JsonCreator
  public TableDef(
      @JsonProperty("description") String description,
      @JsonProperty("inherits") List<String> inherits,
      @JsonProperty("profiles") @JsonAlias({"subsets", "templates"}) List<String> profiles,
      @JsonProperty("semantics") List<String> semantics,
      @JsonProperty("internal") Boolean internal,
      @JsonProperty("label") String label,
      @JsonProperty("oldName") String oldName,
      @JsonProperty("importSchema") String importSchema,
      @JsonProperty("variants") @JsonAlias("subtypes") Map<String, VariantDef> variants,
      @JsonProperty("columns") Map<String, DataColumn> columns,
      @JsonProperty("sections") Map<String, SectionDef> sections) {
    this.description = description;
    this.inherits = inherits != null ? inherits : List.of();
    this.profiles = profiles != null ? profiles : List.of();
    this.semantics = semantics;
    this.internal = internal;
    this.label = label;
    this.oldName = oldName;
    this.importSchema = importSchema;
    this.variants = variants != null ? variants : Map.of();
    this.columns = columns != null ? columns : Map.of();
    this.sections = sections != null ? sections : Map.of();
  }

  public boolean isInternal() {
    return Boolean.TRUE.equals(internal);
  }
}
