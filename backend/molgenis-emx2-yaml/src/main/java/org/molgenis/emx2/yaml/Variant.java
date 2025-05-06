package org.molgenis.emx2.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Variant {
  // name of the variant
  private String variant;

  // label of the variant
  private String label;
  private Map<String, String> labels;
  // description of the variant
  private String description;
  private Map<String, String> descriptions;

  @JsonProperty("extends")
  @JsonSerialize(using = Yaml2Loader.SingleValueListSerializer.class)
  private List<String> extendsVariants;

  // expression that indicates if fields in this variant should be shown
  private String when;

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions;
  }

  public List<String> getExtendsVariants() {
    return extendsVariants;
  }

  public void setExtendsVariants(List<String> extendsVariants) {
    this.extendsVariants = extendsVariants;
  }

  public String getWhen() {
    return when;
  }

  public void setWhen(String when) {
    this.when = when;
  }

  public void overrideProperties(Variant overrideVariant) {
    if (overrideVariant.label != null) {
      this.label = overrideVariant.label;
    }

    if (overrideVariant.labels != null) {
      if (this.labels == null) {
        this.labels = new LinkedHashMap<>();
      }
      this.labels.putAll(overrideVariant.labels);
    }

    if (overrideVariant.description != null) {
      this.description = overrideVariant.description;
    }

    if (overrideVariant.descriptions != null) {
      if (this.descriptions == null) {
        this.descriptions = new LinkedHashMap<>();
      }
      this.descriptions.putAll(overrideVariant.descriptions);
    }

    if (overrideVariant.extendsVariants != null) {
      this.extendsVariants = overrideVariant.extendsVariants;
    }

    if (overrideVariant.when != null) {
      this.when = overrideVariant.when;
    }
  }
}
