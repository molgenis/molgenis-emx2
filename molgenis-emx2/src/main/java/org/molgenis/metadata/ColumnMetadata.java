package org.molgenis.metadata;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.molgenis.MolgenisException;
import static org.molgenis.metadata.Type.STRING;

public class ColumnMetadata {
  private TableMetadata table;

  private String name;
  private Type type;
  private boolean nullable = false;
  private String refTable;
  private String refColumn;
  private String reverseName;
  private String reverseRefColumn;
  private String joinTable;
  private boolean readonly;
  private String visible;
  private String description;
  private String validation;
  private String defaultValue;

  public ColumnMetadata(TableMetadata table, String name, Type type) {
    this.table = table;
    this.name = name;
    this.type = type;
  }

  public ColumnMetadata addColumn(String name) throws MolgenisException {
    return this.getTable().addColumn(name, STRING);
  }

  public ColumnMetadata addColumn(String name, Type type) throws MolgenisException {
    return this.getTable().addColumn(name, type);
  }

  public ColumnMetadata addRef(String name, String toTable) throws MolgenisException {
    return this.getTable().addRef(name, toTable);
  }

  public ColumnMetadata addRef(String name, String toTable, String toColumn)
      throws MolgenisException {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  public ColumnMetadata addRefArray(String name, String toTable) throws MolgenisException {
    return this.getTable().addRefArray(name, toTable);
  }

  public ColumnMetadata addRefArray(String name, String toTable, String toColumn)
      throws MolgenisException {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  public ColumnMetadata primaryKey() throws MolgenisException {
    this.table.setPrimaryKey(this.name);
    return this;
  }

  public TableMetadata getTable() {
    return table;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public String getRefTableName() {
    return refTable;
  }

  public String getRefColumnName() {
    return this.refColumn;
  }

  public String getReverseColumnName() {
    return this.reverseName;
  }

  public String getReverseRefColumn() {
    return this.reverseRefColumn;
  }

  public ColumnMetadata getRefColumn() throws MolgenisException {
    if (getRefColumnName() == null) return null;
    else
      return getTable()
          .getSchema()
          .getTableMetadata(getRefTableName())
          .getColumn(getRefColumnName());
  }

  public ColumnMetadata setNullable(boolean nillable) {
    this.nullable = nillable;
    return this;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (Type.REF.equals(getType()))
      builder.append("ref(").append(refTable).append(",").append(refColumn).append(")");
    else builder.append(getType().toString().toLowerCase());
    if (Boolean.TRUE.equals(getNullable())) builder.append(" setNullable");
    return builder.toString();
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  public String getDescription() {
    return description;
  }

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

  public Boolean isUnique() {
    return getTable().isUnique(getName());
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ColumnMetadata setUnique(boolean unique) throws MolgenisException {
    if (unique) getTable().addUnique(this.getName());
    else getTable().removeUnique(this.getName());
    return this;
  }

  public String getMrefJoinTableName() {
    return joinTable;
  }

  public ColumnMetadata setReference(String refTable, String refColumn) {
    this.refTable = refTable;
    this.refColumn = refColumn;
    return this;
  }

  public ColumnMetadata setReverseReference(String reverseName, String reverseRefColumn) {
    this.reverseName = reverseName;
    this.reverseRefColumn = reverseRefColumn;
    return this;
  }

  public ColumnMetadata setJoinTable(String joinTableName) {
    this.joinTable = joinTableName;
    return this;
  }

  public TableMetadata addUnique(String... columnNames) throws MolgenisException {
    return getTable().addUnique(columnNames);
  }

  public TableMetadata setPrimaryKey(String... columnNames) throws MolgenisException {
    return getTable().setPrimaryKey(columnNames);
  }
}
