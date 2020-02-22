package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.MolgenisException;

import static org.molgenis.emx2.io.emx1.Emx1Import.EMX_1_IMPORT_FAILED;

public class Emx1Entity {
  private String name;
  private String packageName;
  private String extnds;
  private Boolean isAbstract;
  private String label;
  private String description;

  public Emx1Entity(Row row) {
    this.name = row.getString("name");
    if (this.name == null) {
      throw new MolgenisException(EMX_1_IMPORT_FAILED, "attribute name cannot be null");
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
