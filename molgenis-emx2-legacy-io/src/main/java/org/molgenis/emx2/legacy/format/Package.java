package org.molgenis.emx2.legacy.format;

import org.molgenis.emx2.Row;

public class Package {
  private String name;
  private String description;
  private String label;
  private String parent;

  public Package(Row row) {
    this.name = row.getString("name");
    this.description = row.getString("description");
    this.label = row.getString("label");
    this.parent = row.getString("parent");
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getLabel() {
    return label;
  }

  public String getParent() {
    return parent;
  }
}
