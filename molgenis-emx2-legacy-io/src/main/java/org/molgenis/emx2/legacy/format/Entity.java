package org.molgenis.emx2.legacy.format;

import org.molgenis.emx2.Row;

public class Entity {
  private String name;
  private String packageName;
  private String extnds;
  private Boolean isAbstract;
  private String label;
  private String description;

  public Entity(Row row) {
    this.name = row.getString("name");
    this.extnds = row.getString("extends");
    this.packageName = row.getString("package");
    this.isAbstract = row.getBoolean("abstract");
    this.label = row.getString("label");
    this.description = row.getString("description");
  }

  public String getName() {
    return name;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getExtends() {
    return extnds;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

  public Boolean getAbstract() {
    return isAbstract;
  }
}
