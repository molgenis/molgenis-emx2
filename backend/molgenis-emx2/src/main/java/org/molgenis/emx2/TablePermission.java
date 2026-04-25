package org.molgenis.emx2;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public record TablePermission(
    String schema,
    String table,
    Set<TablePermission.SelectScope> select,
    TablePermission.UpdateScope insert,
    TablePermission.UpdateScope update,
    TablePermission.UpdateScope delete,
    boolean changeOwner,
    boolean changeGroup) {

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
    AGGREGATE,
    RANGE,
    OWN,
    GROUP,
    ALL;

    public int permissivenessLevel() {
      return switch (this) {
        case NONE -> 0;
        case EXISTS -> 1;
        case COUNT -> 2;
        case RANGE -> 3;
        case AGGREGATE -> 4;
        case OWN -> 5;
        case GROUP -> 6;
        case ALL -> 7;
      };
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

  public TablePermission {
    if (schema == null) schema = "*";
    if (table == null) table = "*";
    if (select == null || select.isEmpty()) {
      select = Collections.emptySet();
    } else {
      select = Collections.unmodifiableSet(EnumSet.copyOf(select));
    }
    if (insert == null) insert = UpdateScope.NONE;
    if (update == null) update = UpdateScope.NONE;
    if (delete == null) delete = UpdateScope.NONE;
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
}
