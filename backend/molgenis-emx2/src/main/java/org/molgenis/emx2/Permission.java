package org.molgenis.emx2;

import java.util.Objects;

public class Permission {
  private String schema;
  private String table;
  private SelectLevel select;
  private ModifyLevel insert;
  private ModifyLevel update;
  private ModifyLevel delete;
  private ColumnAccess columnAccess;
  private Boolean grant;

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

  public SelectLevel getSelect() {
    return select;
  }

  public Permission setSelect(SelectLevel select) {
    this.select = select;
    return this;
  }

  public ModifyLevel getInsert() {
    return insert;
  }

  public Permission setInsert(ModifyLevel insert) {
    this.insert = insert;
    return this;
  }

  public ModifyLevel getUpdate() {
    return update;
  }

  public Permission setUpdate(ModifyLevel update) {
    this.update = update;
    return this;
  }

  public ModifyLevel getDelete() {
    return delete;
  }

  public Permission setDelete(ModifyLevel delete) {
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

  public Boolean getGrant() {
    return grant;
  }

  public Permission setGrant(Boolean grant) {
    this.grant = grant;
    return this;
  }

  public boolean isRevocation() {
    return select == null
        && insert == null
        && update == null
        && delete == null
        && grant == null
        && columnAccess == null;
  }

  public boolean hasRowLevelPermissions() {
    return select == SelectLevel.ROW
        || insert == ModifyLevel.ROW
        || update == ModifyLevel.ROW
        || delete == ModifyLevel.ROW;
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
        + ", grant="
        + grant
        + '}';
  }
}
