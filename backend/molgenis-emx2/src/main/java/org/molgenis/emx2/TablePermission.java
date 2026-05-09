package org.molgenis.emx2;

import java.util.Objects;

public class TablePermission {
  private final String table;
  private PermissionSet.SelectScope select;
  private PermissionSet.UpdateScope insert;
  private PermissionSet.UpdateScope update;
  private PermissionSet.UpdateScope delete;

  public TablePermission(String table) {
    this.table = table;
  }

  public String table() {
    return table;
  }

  public PermissionSet.SelectScope select() {
    return select;
  }

  public PermissionSet.UpdateScope insert() {
    return insert;
  }

  public PermissionSet.UpdateScope update() {
    return update;
  }

  public PermissionSet.UpdateScope delete() {
    return delete;
  }

  public TablePermission select(PermissionSet.SelectScope select) {
    this.select = select;
    return this;
  }

  public TablePermission insert(PermissionSet.UpdateScope insert) {
    this.insert = insert;
    return this;
  }

  public TablePermission update(PermissionSet.UpdateScope update) {
    this.update = update;
    return this;
  }

  public TablePermission delete(PermissionSet.UpdateScope delete) {
    this.delete = delete;
    return this;
  }

  public PermissionSet.SelectScope getSelect() {
    return select;
  }

  public PermissionSet.UpdateScope getInsert() {
    return insert;
  }

  public PermissionSet.UpdateScope getUpdate() {
    return update;
  }

  public PermissionSet.UpdateScope getDelete() {
    return delete;
  }

  public TablePermission setSelect(PermissionSet.SelectScope select) {
    this.select = select == null ? PermissionSet.SelectScope.NONE : select;
    return this;
  }

  public TablePermission setInsert(PermissionSet.UpdateScope insert) {
    this.insert = insert == null ? PermissionSet.UpdateScope.NONE : insert;
    return this;
  }

  public TablePermission setUpdate(PermissionSet.UpdateScope update) {
    this.update = update == null ? PermissionSet.UpdateScope.NONE : update;
    return this;
  }

  public TablePermission setDelete(PermissionSet.UpdateScope delete) {
    this.delete = delete == null ? PermissionSet.UpdateScope.NONE : delete;
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
