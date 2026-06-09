package org.molgenis.emx2;

import java.util.Objects;

public class TablePermission {
  private final String table;
  private Boolean select;
  private Boolean insert;
  private Boolean update;
  private Boolean delete;

  public TablePermission(String table) {
    this.table = table;
  }

  public String table() {
    return table;
  }

  public Boolean select() {
    return select;
  }

  public Boolean insert() {
    return insert;
  }

  public Boolean update() {
    return update;
  }

  public Boolean delete() {
    return delete;
  }

  public boolean hasSelect() {
    return Boolean.TRUE.equals(select);
  }

  public boolean hasInsert() {
    return Boolean.TRUE.equals(insert);
  }

  public boolean hasUpdate() {
    return Boolean.TRUE.equals(update);
  }

  public boolean hasDelete() {
    return Boolean.TRUE.equals(delete);
  }

  public TablePermission select(Boolean select) {
    this.select = select;
    return this;
  }

  public TablePermission insert(Boolean insert) {
    this.insert = insert;
    return this;
  }

  public TablePermission update(Boolean update) {
    this.update = update;
    return this;
  }

  public TablePermission delete(Boolean delete) {
    this.delete = delete;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TablePermission that = (TablePermission) o;
    return Objects.equals(table, that.table)
        && Objects.equals(select, that.select)
        && Objects.equals(insert, that.insert)
        && Objects.equals(update, that.update)
        && Objects.equals(delete, that.delete);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, select, insert, update, delete);
  }

  @Override
  public String toString() {
    return "TablePermission{"
        + "table='"
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
        + '}';
  }
}
