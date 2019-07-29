package org.molgenis.beans;

import org.molgenis.*;

public class ColumnBean implements Column {
  private Table table;
  private String name;
  private Type type;
  private boolean nullable;
  private String refTable;
  private String refColumn;
  private boolean readonly;
  private String visible;
  private String description;
  private String validation;
  private String defaultValue;

  //    @Id private String name;
  //    private EmxTable table;
  //    private EmxType type = EmxType.STRING;
  //    private Boolean nillable = false;
  //    private Boolean readonly = false;
  //    private Boolean unique = false;
  //    private String defaultValue;
  //    private String description;
  //    private String validation;
  //    private String visible;
  //    private EmxTable ref;
  //    private EmxTable joinTable;
  //    private EmxColumn joinColumn;

  public ColumnBean(String name) {
    this.name = name;
    this.type = Type.STRING;
  }

  public ColumnBean(Table table, String name, Type type) {
    this.table = table;
    this.name = name;
    this.type = type;
  }

  public ColumnBean(Table table, String name, Type type, String otherTable, String otherColumn) {
    this.table = table;
    this.name = name;
    this.type = type;
    this.refTable = otherTable;
    this.refColumn = otherColumn;
  }

  @Override
  public Column addColumn(String name, Type type) throws MolgenisException {
    return this.getTable().addColumn(name, type);
  }

  @Override
  public Table getTable() {
    return table;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Boolean isNullable() {
    return nullable;
  }

  @Override
  public String getRefTable() {
    return refTable;
  }

  //  @Override
  //  public Column setRefTable(String table) {
  //    this.refTable = table;
  //    return this;
  //  }

  @Override
  public String getRefColumn() {
    return refColumn;
  }

  @Override
  public Column setNullable(boolean nillable) throws MolgenisException {
    this.nullable = nillable;
    return this;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (Type.REF.equals(getType())) builder.append("ref(").append(refTable).append(")");
    else builder.append(getType().toString().toLowerCase());
    if (isNullable()) builder.append(" nullable");
    return builder.toString();
  }

  @Override
  public Boolean isReadonly() {
    return readonly;
  }

  @Override
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  public String getVisible() {
    return visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }

  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  @Override
  public Boolean isUnique() {
    return getTable().isUnique(getName());
  }

  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
}
