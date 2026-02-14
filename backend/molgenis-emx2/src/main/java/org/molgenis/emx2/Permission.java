package org.molgenis.emx2;

import java.util.List;
import java.util.Objects;

public class Permission {
  private String schema;
  private String table;
  private boolean isRowLevel;
  private Boolean select;
  private Boolean insert;
  private Boolean update;
  private Boolean delete;
  private List<String> editColumns;
  private List<String> denyColumns;

  public Permission() {}

  public Permission(String tableName, boolean isRowLevel) {
    this.table = tableName;
    this.isRowLevel = isRowLevel;
  }

  public String getSchema() {
    return schema;
  }

  public Permission setSchema(String schemaName) {
    this.schema = schemaName;
    return this;
  }

  public String getTable() {
    return table;
  }

  public Permission setTable(String tableName) {
    this.table = tableName;
    return this;
  }

  public boolean isRowLevel() {
    return isRowLevel;
  }

  public Permission setRowLevel(boolean isRowLevel) {
    this.isRowLevel = isRowLevel;
    return this;
  }

  public Boolean getSelect() {
    return select;
  }

  public Permission setSelect(Boolean select) {
    this.select = select;
    return this;
  }

  public Boolean getInsert() {
    return insert;
  }

  public Permission setInsert(Boolean insert) {
    this.insert = insert;
    return this;
  }

  public Boolean getUpdate() {
    return update;
  }

  public Permission setUpdate(Boolean update) {
    this.update = update;
    return this;
  }

  public Boolean getDelete() {
    return delete;
  }

  public Permission setDelete(Boolean delete) {
    this.delete = delete;
    return this;
  }

  public List<String> getEditColumns() {
    return editColumns;
  }

  public Permission setEditColumns(List<String> editColumns) {
    this.editColumns = editColumns;
    return this;
  }

  public List<String> getDenyColumns() {
    return denyColumns;
  }

  public Permission setDenyColumns(List<String> denyColumns) {
    this.denyColumns = denyColumns;
    return this;
  }

  public boolean isRevocation() {
    return (select == null || !select)
        && (insert == null || !insert)
        && (update == null || !update)
        && (delete == null || !delete)
        && editColumns == null
        && denyColumns == null;
  }

  public boolean isSchemaWide() {
    return table == null;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Permission that = (Permission) o;
    return isRowLevel == that.isRowLevel
        && Objects.equals(schema, that.schema)
        && Objects.equals(table, that.table);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, table, isRowLevel);
  }

  @Override
  public String toString() {
    return "Permission{"
        + "schema='"
        + schema
        + '\''
        + ", table='"
        + table
        + '\''
        + ", isRowLevel="
        + isRowLevel
        + ", select="
        + select
        + ", insert="
        + insert
        + ", update="
        + update
        + ", delete="
        + delete
        + ", editColumns="
        + editColumns
        + ", denyColumns="
        + denyColumns
        + '}';
  }
}
