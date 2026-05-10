package org.molgenis.emx2;

import java.util.Objects;

public class TablePermission {

  private final String table;
  private PermissionSet.SelectScope select = PermissionSet.SelectScope.NONE;
  private PermissionSet.UpdateScope insert = PermissionSet.UpdateScope.NONE;
  private PermissionSet.UpdateScope update = PermissionSet.UpdateScope.NONE;
  private PermissionSet.UpdateScope delete = PermissionSet.UpdateScope.NONE;
  private PermissionSet.ReferenceScope reference = PermissionSet.ReferenceScope.NONE;

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
    Objects.requireNonNull(select, "select scope must not be null");
    this.select = select;
    return this;
  }

  public TablePermission insert(PermissionSet.UpdateScope insert) {
    Objects.requireNonNull(insert, "insert scope must not be null");
    this.insert = insert;
    return this;
  }

  public TablePermission update(PermissionSet.UpdateScope update) {
    Objects.requireNonNull(update, "update scope must not be null");
    this.update = update;
    return this;
  }

  public TablePermission delete(PermissionSet.UpdateScope delete) {
    Objects.requireNonNull(delete, "delete scope must not be null");
    this.delete = delete;
    return this;
  }

  public TablePermission reference(PermissionSet.ReferenceScope reference) {
    Objects.requireNonNull(reference, "reference scope must not be null");
    this.reference = reference;
    return this;
  }

  public PermissionSet.ReferenceScope reference() {
    return reference;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof TablePermission other)) return false;
    return Objects.equals(table, other.table)
        && select == other.select
        && insert == other.insert
        && update == other.update
        && delete == other.delete
        && reference == other.reference;
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, select, insert, update, delete, reference);
  }
}
