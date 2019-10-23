package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.MolgenisException;

public class Entity {
  private String name;
  private String packageName;
  private String extnds;
  private Boolean isAbstract;
  private String label;
  private String description;

  public Entity(Row row) {
    this.name = row.getString("name");
    if (this.name == null) {
      throw new MolgenisException("", "", "name cannot be null");
    }
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
