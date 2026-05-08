package org.molgenis.emx2;

import java.util.Objects;

public class TablePermission {
  private final String table;
  private SelectScope select;
  private UpdateScope insert;
  private UpdateScope update;
  private UpdateScope delete;

  public TablePermission(String table) {
    this.table = table;
  }

  public String table() {
    return table;
  }

  public SelectScope select() {
    return select;
  }

  public UpdateScope insert() {
    return insert;
  }

  public UpdateScope update() {
    return update;
  }

  public UpdateScope delete() {
    return delete;
  }

  public TablePermission select(SelectScope select) {
    this.select = select;
    return this;
  }

  public TablePermission insert(UpdateScope insert) {
    this.insert = insert;
    return this;
  }

  public TablePermission update(UpdateScope update) {
    this.update = update;
    return this;
  }

  public TablePermission delete(UpdateScope delete) {
    this.delete = delete;
    return this;
  }

  public SelectScope getSelect() {
    return select;
  }

  public UpdateScope getInsert() {
    return insert;
  }

  public UpdateScope getUpdate() {
    return update;
  }

  public UpdateScope getDelete() {
    return delete;
  }

  public TablePermission setSelect(SelectScope select) {
    this.select = select == null ? SelectScope.NONE : select;
    return this;
  }

  public TablePermission setInsert(UpdateScope insert) {
    this.insert = insert == null ? UpdateScope.NONE : insert;
    return this;
  }

  public TablePermission setUpdate(UpdateScope update) {
    this.update = update == null ? UpdateScope.NONE : update;
    return this;
  }

  public TablePermission setDelete(UpdateScope delete) {
    this.delete = delete == null ? UpdateScope.NONE : delete;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof TablePermission other)) return false;
    return Objects.equals(table, other.table)
        && select == other.select
        && insert == other.insert
        && update == other.update
        && delete == other.delete;
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, select, insert, update, delete);
  }
}
