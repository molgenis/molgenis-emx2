package org.molgenis.emx2;

import org.javers.core.metamodel.annotation.Id;

public class EmxColumn {
  @Id private String name;
  private EmxTable table;
  private EmxType type = EmxType.STRING;
  private Boolean nillable = false;
  private Boolean readonly = false;
  private String defaultValue;
  private EmxColumn ref;
  private EmxTable joinTable;
  private EmxColumn joinColumn;
  private Boolean unique = false;
  private String description;
  private String validation;
  private String visible;

  public EmxColumn(EmxTable table, String name, EmxType type) {
    this.table = table;
    this.name = name;
    this.type = type;
  }

  public EmxTable getTable() {
    return table;
  }

  public String getName() {
    return name;
  }

  public EmxType getType() {
    return type;
  }

  public Boolean getNillable() {
    return nillable;
  }

  public void setNillable(Boolean nillable) {
    this.nillable = nillable;
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setUnique(Boolean b) {
    this.unique = unique;
  }

  public Boolean getUnique() {
    return unique;
  }

  public EmxColumn getRef() {
    return ref;
  }

  public void setRef(EmxColumn ref) {
    this.ref = ref;
  }

  public EmxTable getJoinTable() {
    return joinTable;
  }

  public void setJoinTable(EmxTable joinTable) {
    this.joinTable = joinTable;
  }

  public EmxColumn getJoinColumn() {
    return joinColumn;
  }

  public void setJoinColumn(EmxColumn joinColumn) {
    this.joinColumn = joinColumn;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  public String getVisible() {
    return visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }

  public String toString() {
    return getName();
  }

  public String print() {
    StringBuilder sb = new StringBuilder();
    sb.append("EmxColumn(table='").append(table.getName()).append("'");
    sb.append(" name='").append(name).append("'");
    if (!EmxType.STRING.equals(this.getType()))
      sb.append(" ").append(type.toString().toLowerCase());
    if (this.getNillable()) sb.append(" nillable");
    if (this.getReadonly()) sb.append(" readonly");
    if (this.getUnique()) sb.append(" unique");
    sb.append(")");
    return sb.toString();
  }
}
