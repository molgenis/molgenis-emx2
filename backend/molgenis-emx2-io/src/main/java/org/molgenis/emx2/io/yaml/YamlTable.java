package org.molgenis.emx2.io.yaml;

import java.util.Map;

public class YamlTable extends YamlBaseContainer {
  private String plural; // default adds 's'
  private Map<String, YamlColumn> columns;
  private Map<String, String> standards;

  public Map<String, YamlColumn> getColumns() {
    return columns;
  }

  public void setColumns(Map<String, YamlColumn> columns) {
    this.columns = columns;
  }

  public String getPlural() {
    return plural;
  }

  public void setPlural(String plural) {
    this.plural = plural;
  }

  public Map<String, String> getStandards() {
    return standards;
  }

  public void setStandards(Map<String, String> standards) {
    this.standards = standards;
  }
}
