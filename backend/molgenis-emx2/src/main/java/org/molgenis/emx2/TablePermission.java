package org.molgenis.emx2;

import java.util.Locale;

public record TablePermission(
    String schema,
    String table,
    TablePermission.Select select,
    TablePermission.Scope insert,
    TablePermission.Scope update,
    TablePermission.Scope delete,
    boolean changeOwner,
    boolean share) {

  public enum Scope {
    NONE,
    OWN,
    GROUP,
    ALL;

    public static Scope fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (Scope s : values()) {
        if (s.name().equals(upper)) {
          return s;
        }
      }
      throw new MolgenisException("Unknown Scope: " + name);
    }
  }

  public enum Select {
    NONE,
    EXISTS,
    COUNT,
    AGGREGATE,
    RANGE,
    OWN,
    GROUP,
    ALL;

    public int permissivenessLevel() {
      switch (this) {
        case NONE:
          return 0;
        case EXISTS:
          return 1;
        case COUNT:
          return 2;
        case RANGE:
          return 3;
        case AGGREGATE:
          return 4;
        case OWN:
          return 5;
        case GROUP:
          return 6;
        case ALL:
          return 7;
        default:
          throw new IllegalStateException("unhandled Select: " + this);
      }
    }

    public static Select fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (Select s : values()) {
        if (s.name().equals(upper)) {
          return s;
        }
      }
      throw new MolgenisException("Unknown Select: " + name);
    }
  }

  public TablePermission {
    if (schema == null) schema = "*";
    if (table == null) table = "*";
    if (select == null) select = Select.NONE;
    if (insert == null) insert = Scope.NONE;
    if (update == null) update = Scope.NONE;
    if (delete == null) delete = Scope.NONE;
  }
}
