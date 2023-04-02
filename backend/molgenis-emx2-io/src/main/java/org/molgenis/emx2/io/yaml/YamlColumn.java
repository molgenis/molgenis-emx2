package org.molgenis.emx2.io.yaml;

import java.util.Map;

public class YamlColumn extends YamlBase {
  private String type;
  private Integer key;
  private String label;
  private Boolean required;
  private String refTable;
  private String refBack;
  private boolean allowOther;
  private String examples;
  // for reusing purposes
  private String includeArchetype;
  private Map<String, YamlColumn> includeColumns;

  public String getIncludeArchetype() {
    return includeArchetype;
  }

  public void setIncludeArchetype(String includeArchetype) {
    this.includeArchetype = includeArchetype;
  }

  public Map<String, YamlColumn> getIncludeColumns() {
    return includeColumns;
  }

  public void setIncludeColumns(Map<String, YamlColumn> columns) {
    this.includeColumns = includeColumns;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  public String getRefTable() {
    return refTable;
  }

  public void setRefTable(String refTable) {
    this.refTable = refTable;
  }

  public String getRefBack() {
    return refBack;
  }

  public void setRefBack(String refBack) {
    this.refBack = refBack;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isAllowOther() {
    return allowOther;
  }

  public void setAllowOther(boolean allowOther) {
    this.allowOther = allowOther;
  }

  public String getExamples() {
    return examples;
  }

  public void setExamples(String examples) {
    this.examples = examples;
  }
}
