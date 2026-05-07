package org.molgenis.emx2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PermissionSet {

  public enum SelectScope {
    NONE,
    EXISTS,
    COUNT,
    RANGE,
    AGGREGATE,
    OWN,
    GROUP,
    ALL;

    public boolean allowsRowAccess() {
      return this == ALL || this == OWN || this == GROUP;
    }

    public boolean allowsCount() {
      return this == ALL
          || this == OWN
          || this == GROUP
          || this == COUNT
          || this == AGGREGATE
          || this == RANGE;
    }

    public boolean allowsMinMax() {
      return this == ALL || this == OWN || this == GROUP || this == AGGREGATE || this == RANGE;
    }

    public boolean allowsAvgSum() {
      return this == ALL || this == OWN || this == GROUP || this == AGGREGATE;
    }

    public boolean allowsGroupBy() {
      return this == ALL || this == OWN || this == GROUP || this == AGGREGATE;
    }

    public boolean allowsExactCount() {
      return this == ALL || this == OWN || this == GROUP || this == COUNT || this == AGGREGATE;
    }

    public static SelectScope fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (SelectScope scope : values()) {
        if (scope.name().equals(upper)) {
          return scope;
        }
      }
      throw new MolgenisException("Unknown SelectScope: " + name);
    }
  }

  public enum UpdateScope {
    NONE,
    OWN,
    GROUP,
    ALL;

    public static UpdateScope fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (UpdateScope scope : values()) {
        if (scope.name().equals(upper)) {
          return scope;
        }
      }
      throw new MolgenisException("Unknown UpdateScope: " + name);
    }
  }

  public static class TablePermissions {
    private SelectScope select = SelectScope.NONE;
    private UpdateScope insert = UpdateScope.NONE;
    private UpdateScope update = UpdateScope.NONE;
    private UpdateScope delete = UpdateScope.NONE;

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

    public TablePermissions setSelect(SelectScope select) {
      this.select = select == null ? SelectScope.NONE : select;
      return this;
    }

    public TablePermissions setInsert(UpdateScope insert) {
      this.insert = insert == null ? UpdateScope.NONE : insert;
      return this;
    }

    public TablePermissions setUpdate(UpdateScope update) {
      this.update = update == null ? UpdateScope.NONE : update;
      return this;
    }

    public TablePermissions setDelete(UpdateScope delete) {
      this.delete = delete == null ? UpdateScope.NONE : delete;
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
  private String description = "";
  private String schema = null;

  public Map<String, TablePermissions> getTables() {
    return Collections.unmodifiableMap(tables);
  }

  public boolean isChangeOwner() {
    return changeOwner;
  }

  public boolean isChangeGroup() {
    return changeGroup;
  }

  public String getDescription() {
    return description;
  }

  public String getSchema() {
    return schema;
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

  public PermissionSet setDescription(String description) {
    this.description = description == null ? "" : description;
    return this;
  }

  public PermissionSet setSchema(String schema) {
    this.schema = schema;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PermissionSet other)) return false;
    return changeOwner == other.changeOwner
        && changeGroup == other.changeGroup
        && Objects.equals(description, other.description)
        && Objects.equals(tables, other.tables)
        && Objects.equals(schema, other.schema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tables, changeOwner, changeGroup, description, schema);
  }
}
