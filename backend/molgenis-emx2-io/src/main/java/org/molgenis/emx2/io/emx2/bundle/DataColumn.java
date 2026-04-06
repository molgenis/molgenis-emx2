package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataColumn(
    String type,
    Integer key,
    Object required,
    String defaultValue,
    String validation,
    String visible,
    String computed,
    Boolean readonly,
    String refTable,
    String refLink,
    String refBack,
    String refLabel,
    String refSchema,
    Integer position,
    String description,
    List<String> semantics,
    String subtype,
    List<String> profiles,
    String label,
    String oldName,
    Boolean drop) {

  @JsonCreator
  public DataColumn(
      @JsonProperty("type") String type,
      @JsonProperty("key") Integer key,
      @JsonProperty("required") Object required,
      @JsonProperty("defaultValue") String defaultValue,
      @JsonProperty("validation") String validation,
      @JsonProperty("visible") String visible,
      @JsonProperty("computed") String computed,
      @JsonProperty("readonly") Boolean readonly,
      @JsonProperty("refTable") String refTable,
      @JsonProperty("refLink") String refLink,
      @JsonProperty("refBack") String refBack,
      @JsonProperty("refLabel") String refLabel,
      @JsonProperty("refSchema") String refSchema,
      @JsonProperty("position") Integer position,
      @JsonProperty("description") String description,
      @JsonProperty("semantics") List<String> semantics,
      @JsonProperty("subtype") String subtype,
      @JsonProperty("profiles") @JsonAlias({"subsets", "templates"}) List<String> profiles,
      @JsonProperty("label") String label,
      @JsonProperty("oldName") String oldName,
      @JsonProperty("drop") Boolean drop) {
    this.type = type;
    this.key = key;
    this.required = required;
    this.defaultValue = defaultValue;
    this.validation = validation;
    this.visible = visible;
    this.computed = computed;
    this.readonly = readonly;
    this.refTable = refTable;
    this.refLink = refLink;
    this.refBack = refBack;
    this.refLabel = refLabel;
    this.refSchema = refSchema;
    this.position = position;
    this.description = description;
    this.semantics = semantics;
    this.subtype = subtype;
    this.profiles = profiles;
    this.label = label;
    this.oldName = oldName;
    this.drop = drop;
  }

  public static DataColumn empty() {
    return new DataColumn(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null);
  }
}
