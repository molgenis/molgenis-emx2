package org.molgenis.emx2.io.yaml;

import java.util.Map;

public class YamlSchema extends YamlBaseContainer {
  String version;
  Map<String, YamlTable> tables;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Map<String, YamlTable> getTables() {
    return tables;
  }

  public void setTables(Map<String, YamlTable> tables) {
    this.tables = tables;
  }
}
