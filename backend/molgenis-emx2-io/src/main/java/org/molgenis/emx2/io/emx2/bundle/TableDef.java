package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TableDef(
    String description,
    List<String> extendNames,
    List<String> profiles,
    List<String> semantics,
    Boolean internal,
    String label,
    String oldName,
    String importSchema,
    List<VariantDef> variants,
    List<Map<String, Object>> columns) {

  @JsonCreator
  public TableDef(
      @JsonProperty("description") String description,
      @JsonProperty("extends") List<String> extendNames,
      @JsonProperty("profiles") List<String> profiles,
      @JsonProperty("semantics") @JsonDeserialize(using = StringOrListDeserializer.class)
          List<String> semantics,
      @JsonProperty("internal") Boolean internal,
      @JsonProperty("label") String label,
      @JsonProperty("oldName") String oldName,
      @JsonProperty("importSchema") String importSchema,
      @JsonProperty("variants") List<VariantDef> variants,
      @JsonProperty("columns") List<Map<String, Object>> columns) {
    this.description = description;
    this.extendNames = extendNames != null ? extendNames : List.of();
    this.profiles = profiles != null ? profiles : List.of();
    this.semantics = semantics;
    this.internal = internal;
    this.label = label;
    this.oldName = oldName;
    this.importSchema = importSchema;
    this.variants = variants != null ? variants : List.of();
    this.columns = columns != null ? columns : List.of();
  }

  public boolean isInternal() {
    return Boolean.TRUE.equals(internal);
  }
}
