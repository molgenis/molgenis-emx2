package org.molgenis.emx2;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class TablePermission {

  private String schema;
  private String table;
  private Set<SelectScope> select;
  private UpdateScope insert;
  private UpdateScope update;
  private UpdateScope delete;
  private boolean changeOwner;
  private boolean changeGroup;

  public enum UpdateScope {
    NONE,
    OWN,
    GROUP,
    ALL;

    public static UpdateScope fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (UpdateScope s : values()) {
        if (s.name().equals(upper)) {
          return s;
        }
      }
      throw new MolgenisException("Unknown UpdateScope: " + name);
    }
  }

  public enum SelectScope {
    NONE,
    EXISTS,
    COUNT,
    RANGE,
    AGGREGATE,
    OWN,
    GROUP,
    ALL;

    public int permissivenessLevel() {
      return ordinal();
    }

    public static SelectScope fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (SelectScope s : values()) {
        if (s.name().equals(upper)) {
          return s;
        }
      }
      throw new MolgenisException("Unknown SelectScope: " + name);
    }
  }

  public TablePermission() {
    this.schema = "*";
    this.table = "*";
    this.select = Collections.emptySet();
    this.insert = UpdateScope.NONE;
    this.update = UpdateScope.NONE;
    this.delete = UpdateScope.NONE;
    this.changeOwner = false;
    this.changeGroup = false;
  }

  public TablePermission(String table) {
    this();
    this.table = table == null ? "*" : table;
  }

  public TablePermission(String schema, String table) {
    this();
    this.schema = schema == null ? "*" : schema;
    this.table = table == null ? "*" : table;
  }

  public TablePermission(TablePermission source) {
    this.schema = source.schema;
    this.table = source.table;
    this.select = source.select;
    this.insert = source.insert;
    this.update = source.update;
    this.delete = source.delete;
    this.changeOwner = source.changeOwner;
    this.changeGroup = source.changeGroup;
  }

  public String schema() {
    return schema;
  }

  public String table() {
    return table;
  }

  public Set<SelectScope> select() {
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

  public boolean changeOwner() {
    return changeOwner;
  }

  public boolean changeGroup() {
    return changeGroup;
  }

  public TablePermission setSchema(String schema) {
    this.schema = schema == null ? "*" : schema;
    return this;
  }

  public TablePermission setTable(String table) {
    this.table = table == null ? "*" : table;
    return this;
  }

  public TablePermission select(SelectScope... scopes) {
    if (scopes == null || scopes.length == 0) {
      this.select = Collections.emptySet();
    } else {
      this.select = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(scopes)));
    }
    return this;
  }

  public TablePermission select(Set<SelectScope> scopes) {
    this.select =
        (scopes == null || scopes.isEmpty())
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(scopes));
    return this;
  }

  public TablePermission insert(UpdateScope scope) {
    this.insert = scope == null ? UpdateScope.NONE : scope;
    return this;
  }

  public TablePermission update(UpdateScope scope) {
    this.update = scope == null ? UpdateScope.NONE : scope;
    return this;
  }

  public TablePermission delete(UpdateScope scope) {
    this.delete = scope == null ? UpdateScope.NONE : scope;
    return this;
  }

  public TablePermission setChangeOwner(boolean value) {
    this.changeOwner = value;
    return this;
  }

  public TablePermission setChangeGroup(boolean value) {
    this.changeGroup = value;
    return this;
  }

  public boolean hasRowAccess() {
    return select.contains(SelectScope.ALL)
        || select.contains(SelectScope.OWN)
        || select.contains(SelectScope.GROUP);
  }

  public boolean hasAnySelect() {
    return !select.isEmpty();
  }

  public static Set<SelectScope> singletonSelect(SelectScope scope) {
    if (scope == null || scope == SelectScope.NONE) return Collections.emptySet();
    return EnumSet.of(scope);
  }

  public static Set<SelectScope> emptySelect() {
    return Collections.emptySet();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof TablePermission other)) return false;
    return changeOwner == other.changeOwner
        && changeGroup == other.changeGroup
        && Objects.equals(schema, other.schema)
        && Objects.equals(table, other.table)
        && Objects.equals(select, other.select)
        && insert == other.insert
        && update == other.update
        && delete == other.delete;
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, table, select, insert, update, delete, changeOwner, changeGroup);
  }

  @Override
  public String toString() {
    return "TablePermission["
        + "schema="
        + schema
        + ", table="
        + table
        + ", select="
        + select
        + ", insert="
        + insert
        + ", update="
        + update
        + ", delete="
        + delete
        + ", changeOwner="
        + changeOwner
        + ", changeGroup="
        + changeGroup
        + "]";
  }
}
