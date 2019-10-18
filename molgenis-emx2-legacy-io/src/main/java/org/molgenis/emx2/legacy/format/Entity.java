package org.molgenis.emx2.legacy.format;

import org.molgenis.emx2.Row;

public class Entity {
  private String name;
  private String packageName;
  private String extnds;
  private Boolean isAbstract;
  private String label;
  private String description;

  public Entity(Row row) {}

  public String getName() {
    return name;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getExtnds() {
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
