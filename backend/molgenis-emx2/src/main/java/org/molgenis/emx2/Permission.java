package org.molgenis.emx2;

import java.util.Locale;

public record Permission(
    String schema,
    String table,
    Permission.ViewScope select,
    Permission.EditScope insert,
    Permission.EditScope update,
    Permission.EditScope delete,
    boolean changeOwner,
    boolean share) {

  /**
   * Scope values for SELECT. Wire format is the lowercased constant name (e.g. {@code "own"}).
   * {@link #fromString} accepts both lowercase and uppercase for forgiving client input.
   *
   * <p>Only {@code NONE}, {@code OWN}, {@code GROUP}, and {@code ALL} are currently enforced
   * server-side. The remaining values are declared in the GraphQL schema for future use; the server
   * throws {@link MolgenisException} if any of them is sent in a mutation today.
   */
  public enum ViewScope {
    NONE(true),
    OWN(true),
    GROUP(true),
    ALL(true),
    COUNT_OWN(false),
    COUNT_GROUP(false),
    COUNT_ALL(false),
    AGGREGATE_OWN(false),
    AGGREGATE_GROUP(false),
    AGGREGATE_ALL(false),
    EXISTS_OWN(false),
    EXISTS_GROUP(false),
    EXISTS_ALL(false),
    RANGE_OWN(false),
    RANGE_GROUP(false),
    RANGE_ALL(false);

    private final boolean supported;

    ViewScope(boolean supported) {
      this.supported = supported;
    }

    public boolean isSupported() {
      return supported;
    }

    public boolean atLeast(ViewScope other) {
      return this.ordinal() >= other.ordinal();
    }

    public static ViewScope fromString(String name) {
      String upper = name.toUpperCase(Locale.ROOT);
      for (ViewScope v : values()) {
        if (v.name().equals(upper)) {
          if (!v.supported) {
            throw new MolgenisException("ViewScope " + name + " is not yet supported");
          }
          return v;
        }
      }
      throw new MolgenisException("Unknown ViewScope: " + name);
    }
  }

  public enum EditScope {
    NONE(true),
    OWN(true),
    GROUP(true),
    ALL(true);

    private final boolean supported;

    EditScope(boolean supported) {
      this.supported = supported;
    }

    public boolean isSupported() {
      return supported;
    }

    public boolean atLeast(EditScope other) {
      return this.ordinal() >= other.ordinal();
    }

    public static EditScope fromString(String name) {
      for (EditScope e : values()) {
        if (e.name().equals(name)) {
          if (!e.supported) {
            throw new MolgenisException("EditScope " + name + " is not yet supported");
          }
          return e;
        }
      }
      throw new MolgenisException("Unknown EditScope: " + name);
    }
  }

  public Permission {
    if (schema == null) schema = "*";
    if (table == null) table = "*";
    if (select == null) select = ViewScope.NONE;
    if (insert == null) insert = EditScope.NONE;
    if (update == null) update = EditScope.NONE;
    if (delete == null) delete = EditScope.NONE;
  }
}
