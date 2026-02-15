package org.molgenis.emx2;

import java.util.Objects;

public class Permission {
  private String schema;
  private String table;
  private PermissionLevel select;
  private PermissionLevel insert;
  private PermissionLevel update;
  private PermissionLevel delete;
  private ColumnAccess columnAccess;

  public Permission() {}

  public Permission(String tableName) {
    this.table = tableName;
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

  public PermissionLevel getSelect() {
    return select;
  }

  public Permission setSelect(PermissionLevel select) {
    this.select = select;
    return this;
  }

  public PermissionLevel getInsert() {
    return insert;
  }

  public Permission setInsert(PermissionLevel insert) {
    this.insert = insert;
    return this;
  }

  public PermissionLevel getUpdate() {
    return update;
  }

  public Permission setUpdate(PermissionLevel update) {
    this.update = update;
    return this;
  }

  public PermissionLevel getDelete() {
    return delete;
  }

  public Permission setDelete(PermissionLevel delete) {
    this.delete = delete;
    return this;
  }

  public ColumnAccess getColumnAccess() {
    return columnAccess;
  }

  public Permission setColumnAccess(ColumnAccess columnAccess) {
    this.columnAccess = columnAccess;
    return this;
  }

  public boolean isRevocation() {
    return select == null
        && insert == null
        && update == null
        && delete == null
        && columnAccess == null;
  }

  public boolean hasRowLevelPermissions() {
    return select == PermissionLevel.ROW
        || insert == PermissionLevel.ROW
        || update == PermissionLevel.ROW
        || delete == PermissionLevel.ROW;
  }

  public boolean isSchemaWide() {
    return table == null;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Permission that = (Permission) o;
    return Objects.equals(schema, that.schema) && Objects.equals(table, that.table);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, table);
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
        + ", select="
        + select
        + ", insert="
        + insert
        + ", update="
        + update
        + ", delete="
        + delete
        + ", columnAccess="
        + columnAccess
        + '}';
  }
}
