package org.molgenis.emx2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PermissionSet implements Iterable<Permission> {

  record ValidationError(String schema, String table, String message) {}

  private final LinkedHashMap<String, Permission> entries = new LinkedHashMap<>();

  private static String key(String schema, String table) {
    return schema + ":" + table;
  }

  public void put(Permission permission) {
    entries.put(key(permission.schema(), permission.table()), permission);
  }

  public void remove(String schema, String table) {
    entries.remove(key(schema, table));
  }

  public int size() {
    return entries.size();
  }

  public boolean contains(String schema, String table) {
    return entries.containsKey(key(schema, table));
  }

  @Override
  public Iterator<Permission> iterator() {
    return entries.values().iterator();
  }

  public List<ValidationError> validate(Function<PermissionSet.TableRef, Boolean> isRlsEnabled) {
    List<ValidationError> errors = new ArrayList<>();
    for (Permission p : entries.values()) {
      String schema = p.schema();
      String table = p.table();
      if (p.delete().ordinal() > p.select().ordinal()) {
        errors.add(new ValidationError(schema, table, "delete requires read"));
      }
      if (p.update().ordinal() > p.select().ordinal()) {
        errors.add(new ValidationError(schema, table, "update requires read"));
      }
      if (p.changeOwner() && !p.update().atLeast(Permission.EditScope.OWN)) {
        errors.add(new ValidationError(schema, table, "changeOwner requires update"));
      }
      if (p.share() && !p.update().atLeast(Permission.EditScope.OWN)) {
        errors.add(new ValidationError(schema, table, "share requires update"));
      }
      if (!schema.equals("*") && !table.equals("*")) {
        boolean needsRls =
            p.select() == Permission.ViewScope.OWN
                || p.select() == Permission.ViewScope.GROUP
                || p.insert() == Permission.EditScope.OWN
                || p.insert() == Permission.EditScope.GROUP
                || p.update() == Permission.EditScope.OWN
                || p.update() == Permission.EditScope.GROUP
                || p.delete() == Permission.EditScope.OWN
                || p.delete() == Permission.EditScope.GROUP;
        if (needsRls && !Boolean.TRUE.equals(isRlsEnabled.apply(new TableRef(schema, table)))) {
          errors.add(
              new ValidationError(schema, table, "own/group scope requires row_level_security"));
        }
      }
    }
    return errors;
  }

  public void validateOrThrow(Function<TableRef, Boolean> isRlsEnabled) {
    List<ValidationError> errors = validate(isRlsEnabled);
    if (!errors.isEmpty()) {
      String detail =
          errors.stream()
              .map(e -> "schema '" + e.schema() + "' table '" + e.table() + "': " + e.message())
              .collect(Collectors.joining("; "));
      throw new MolgenisException("Permission validation failed: " + detail);
    }
  }

  public Permission resolveFor(String schemaName, String tableName) {
    Permission wildcardBoth = entries.get(key("*", "*"));
    Permission wildcardSchema = entries.get(key("*", tableName));
    Permission wildcardTable = entries.get(key(schemaName, "*"));
    Permission exact = entries.get(key(schemaName, tableName));

    Permission.ViewScope select = Permission.ViewScope.NONE;
    Permission.EditScope insert = Permission.EditScope.NONE;
    Permission.EditScope update = Permission.EditScope.NONE;
    Permission.EditScope delete = Permission.EditScope.NONE;
    boolean changeOwner = false;
    boolean share = false;

    for (Permission p : new Permission[] {wildcardBoth, wildcardSchema, wildcardTable, exact}) {
      if (p == null) continue;
      if (p.select().ordinal() > select.ordinal()) select = p.select();
      if (p.insert().ordinal() > insert.ordinal()) insert = p.insert();
      if (p.update().ordinal() > update.ordinal()) update = p.update();
      if (p.delete().ordinal() > delete.ordinal()) delete = p.delete();
      if (p.changeOwner()) changeOwner = true;
      if (p.share()) share = true;
    }

    return new Permission(
        schemaName, tableName, select, insert, update, delete, changeOwner, share);
  }

  public record TableRef(String schema, String table) {}
}
