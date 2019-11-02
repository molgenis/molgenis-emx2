package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.Row;

public class Emx1Package {
  private String name;
  private String description;
  private String label;
  private String parent;

  public Emx1Package(Row row) {
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
