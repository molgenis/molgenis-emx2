package org.molgenis.emx2;

import org.javers.core.metamodel.annotation.Id;

public class EmxColumn {
  EmxModel model;
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

  public EmxColumn(EmxModel model, EmxTable table, String name, EmxType type) {
    this.model = model;
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

  public void setNillable(Boolean nillable) throws EmxException {
    this.nillable = nillable;
    model.onColumnChange(this);
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) throws EmxException {
    this.readonly = readonly;
    model.onColumnChange(this);
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) throws EmxException {
    this.defaultValue = defaultValue;
    model.onColumnChange(this);
  }

  public void setUnique(Boolean b) throws EmxException {
    this.unique = unique;
    model.onColumnChange(this);
  }

  public Boolean getUnique() {
    return unique;
  }

  public EmxColumn getRef() {
    return ref;
  }

  public void setRef(EmxColumn ref) throws EmxException {
    this.ref = ref;
    model.onColumnChange(this);
  }

  public EmxTable getJoinTable() {
    return joinTable;
  }

  public void setJoinTable(EmxTable joinTable) throws EmxException {
    this.joinTable = joinTable;
    model.onColumnChange(this);
  }

  public EmxColumn getJoinColumn() {
    return joinColumn;
  }

  public void setJoinColumn(EmxColumn joinColumn) throws EmxException {
    this.joinColumn = joinColumn;
    model.onColumnChange(this);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) throws EmxException {
    this.description = description;
    model.onColumnChange(this);
  }

  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) throws EmxException {
    this.validation = validation;
    model.onColumnChange(this);
  }

  public String getVisible() {
    return visible;
  }

  public void setVisible(String visible) throws EmxException {
    this.visible = visible;
    model.onColumnChange(this);
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
