package org.molgenis.emx2.io.beans;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.molgenis.emx2.EmxColumn;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;

@TypeName("column")
public class EmxColumnBean implements EmxColumn {
  private EmxTable table;
  @Id private String name;
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

  public EmxColumnBean(EmxTable table, String name, EmxType type) {
    this.table = table;
    this.name = name;
    this.type = type;
  }

  @Override
  public EmxTable getTable() {
    return table;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public EmxType getType() {
    return type;
  }

  @Override
  public Boolean getNillable() {
    return nillable;
  }

  public void setNillable(Boolean nillable) {
    this.nillable = nillable;
  }

  @Override
  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public Boolean getUnique() {
    return unique;
  }

  public void setUnique(Boolean unique) {
    this.unique = unique;
  }

  @Override
  public EmxColumn getRef() {
    return ref;
  }

  public void setRef(EmxColumn ref) {
    this.ref = ref;
  }

  @Override
  public EmxTable getJoinTable() {
    return joinTable;
  }

  public void setJoinTable(EmxTable joinTable) {
    this.joinTable = joinTable;
  }

  @Override
  public EmxColumn getJoinColumn() {
    return joinColumn;
  }

  public void setJoinColumn(EmxColumn joinColumn) {
    this.joinColumn = joinColumn;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  @Override
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
