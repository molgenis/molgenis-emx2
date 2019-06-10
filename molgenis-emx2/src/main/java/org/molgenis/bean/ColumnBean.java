package org.molgenis.bean;

import org.molgenis.Column;
import org.molgenis.DatabaseException;
import org.molgenis.Table;

public class ColumnBean implements Column {
  private Table table;
  private String name;
  private Type type;
  private boolean nullable;
  private Table refTable;
  private String mrefTable;
  private String mrefBack;
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

  public ColumnBean(Table table, String name, Table otherTable) {
    this.table = table;
    this.name = name;
    this.type = Type.REF;
    this.refTable = otherTable;
  }

  public ColumnBean(Table table, String name, Table otherTable, String joinTable) {
    this.table = table;
    this.name = name;
    this.type = Type.MREF;
    this.refTable = otherTable;
    this.mrefTable = joinTable;
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
  public Table getRefTable() {
    return refTable;
  }

  @Override
  public Column setRefTable(Table table) {
    this.refTable = table;
    return this;
  }

  @Override
  public Table getMrefTable() {
    return table;
  }

  @Override
  public String getMrefBack() {
    return mrefBack;
  }

  @Override
  public Column setNullable(boolean nillable) throws DatabaseException {
    this.nullable = nillable;
    return this;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (Type.REF.equals(getType())) builder.append("ref(").append(refTable.getName()).append(")");
    else builder.append(getType().toString().toLowerCase());
    if (isNullable()) builder.append(" nullable");
    return builder.toString();
  }

  @Override
  public boolean isReadonly() {
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

  @Override
  public String getVisible() {
    return visible;
  }

  @Override
  public void setVisible(String visible) {
    this.visible = visible;
  }

  @Override
  public String getValidation() {
    return validation;
  }

  @Override
  public void setValidation(String validation) {
    this.validation = validation;
  }

  @Override
  public boolean isUnique() {
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
