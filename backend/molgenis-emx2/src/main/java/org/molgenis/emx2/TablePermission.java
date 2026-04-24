package org.molgenis.emx2;

import java.util.Locale;

public record TablePermission(
    String schema,
    String table,
    TablePermission.Scope select,
    TablePermission.Scope insert,
    TablePermission.Scope update,
    TablePermission.Scope delete,
    boolean changeOwner,
    boolean share,
    TablePermission.ViewMode viewMode) {

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

  public enum ViewMode {
    FULL(true),
    COUNT(false),
    AGGREGATE(false),
    EXISTS(false),
    RANGE(false);

    private final boolean supported;

    ViewMode(boolean supported) {
      this.supported = supported;
    }

    public boolean isSupported() {
      return supported;
    }

    public static ViewMode fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (ViewMode vm : values()) {
        if (vm.name().equals(upper)) {
          if (!vm.supported) {
            throw new MolgenisException("ViewMode " + name + " is not yet supported");
          }
          return vm;
        }
      }
      throw new MolgenisException("Unknown ViewMode: " + name);
    }
  }

  public TablePermission(
      String schema,
      String table,
      Scope select,
      Scope insert,
      Scope update,
      Scope delete,
      boolean changeOwner,
      boolean share) {
    this(schema, table, select, insert, update, delete, changeOwner, share, null);
  }

  public TablePermission {
    if (schema == null) schema = "*";
    if (table == null) table = "*";
    if (select == null) select = Scope.NONE;
    if (insert == null) insert = Scope.NONE;
    if (update == null) update = Scope.NONE;
    if (delete == null) delete = Scope.NONE;
    if (viewMode == null) viewMode = ViewMode.FULL;
  }
}
