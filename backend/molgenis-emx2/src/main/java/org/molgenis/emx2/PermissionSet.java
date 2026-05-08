package org.molgenis.emx2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PermissionSet {

  private Map<String, TablePermission> tables = new LinkedHashMap<>();
  private boolean changeOwner = false;
  private boolean changeGroup = false;
  private String description = "";
  private String schema = null;

  public Map<String, TablePermission> getTables() {
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

  public PermissionSet setTables(Map<String, TablePermission> tables) {
    this.tables = tables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tables);
    return this;
  }

  public PermissionSet putTable(String tableName, TablePermission permissions) {
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
