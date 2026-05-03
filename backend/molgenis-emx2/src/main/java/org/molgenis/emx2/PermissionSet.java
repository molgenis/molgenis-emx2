package org.molgenis.emx2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PermissionSet {

  public static class TablePermissions {
    private SelectScope select = SelectScope.NONE;
    private SelectScope insert = SelectScope.NONE;
    private SelectScope update = SelectScope.NONE;
    private SelectScope delete = SelectScope.NONE;

    public SelectScope getSelect() {
      return select;
    }

    public SelectScope getInsert() {
      return insert;
    }

    public SelectScope getUpdate() {
      return update;
    }

    public SelectScope getDelete() {
      return delete;
    }

    public TablePermissions setSelect(SelectScope select) {
      this.select = select == null ? SelectScope.NONE : select;
      return this;
    }

    public TablePermissions setInsert(SelectScope insert) {
      this.insert = insert == null ? SelectScope.NONE : insert;
      return this;
    }

    public TablePermissions setUpdate(SelectScope update) {
      this.update = update == null ? SelectScope.NONE : update;
      return this;
    }

    public TablePermissions setDelete(SelectScope delete) {
      this.delete = delete == null ? SelectScope.NONE : delete;
      return this;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof TablePermissions other)) return false;
      return select == other.select
          && insert == other.insert
          && update == other.update
          && delete == other.delete;
    }

    @Override
    public int hashCode() {
      return Objects.hash(select, insert, update, delete);
    }
  }

  private Map<String, TablePermissions> tables = new LinkedHashMap<>();
  private boolean changeOwner = false;
  private boolean changeGroup = false;

  public Map<String, TablePermissions> getTables() {
    return Collections.unmodifiableMap(tables);
  }

  public boolean isChangeOwner() {
    return changeOwner;
  }

  public boolean isChangeGroup() {
    return changeGroup;
  }

  public PermissionSet setTables(Map<String, TablePermissions> tables) {
    this.tables = tables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tables);
    return this;
  }

  public PermissionSet putTable(String tableName, TablePermissions permissions) {
    this.tables.put(tableName, permissions);
    return this;
  }

  public PermissionSet setChangeOwner(boolean changeOwner) {
    this.changeOwner = changeOwner;
    return this;
  }

  public PermissionSet setChangeGroup(boolean changeGroup) {
    this.changeGroup = changeGroup;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PermissionSet other)) return false;
    return changeOwner == other.changeOwner
        && changeGroup == other.changeGroup
        && Objects.equals(tables, other.tables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tables, changeOwner, changeGroup);
  }
}
