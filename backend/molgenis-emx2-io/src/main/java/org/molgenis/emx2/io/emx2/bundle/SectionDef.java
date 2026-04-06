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
public record SectionDef(
    String subtype,
    List<String> profiles,
    Map<String, DataColumn> columns,
    Map<String, HeadingDef> headings) {

  @JsonCreator
  public SectionDef(
      @JsonProperty("subtype") String subtype,
      @JsonProperty("profiles") @JsonAlias({"subsets", "templates"}) List<String> profiles,
      @JsonProperty("columns") Map<String, DataColumn> columns,
      @JsonProperty("headings") Map<String, HeadingDef> headings) {
    this.subtype = subtype;
    this.profiles = profiles != null ? profiles : List.of();
    this.columns = columns != null ? columns : Map.of();
    this.headings = headings != null ? headings : Map.of();
  }
}
