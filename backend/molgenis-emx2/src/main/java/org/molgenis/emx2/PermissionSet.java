package org.molgenis.emx2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionSet implements Iterable<TablePermission> {

  record ValidationError(String schema, String table, String message) {}

  private final LinkedHashMap<String, TablePermission> entries = new LinkedHashMap<>();

  private static String key(String schema, String table) {
    return schema + ":" + table;
  }

  public void put(TablePermission permission) {
    entries.put(key(permission.schema(), permission.table()), permission);
  }

  public int size() {
    return entries.size();
  }

  @Override
  public Iterator<TablePermission> iterator() {
    return entries.values().iterator();
  }

  public List<ValidationError> validate() {
    List<ValidationError> errors = new ArrayList<>();
    for (TablePermission p : entries.values()) {
      String schema = p.schema();
      String table = p.table();
      boolean hasReadAccess = p.select() != TablePermission.Select.NONE;
      if (p.delete() != TablePermission.Scope.NONE && !hasReadAccess) {
        errors.add(new ValidationError(schema, table, "delete requires read"));
      }
      if (p.update() != TablePermission.Scope.NONE && !hasReadAccess) {
        errors.add(new ValidationError(schema, table, "update requires read"));
      }
      if (p.changeOwner() && p.update().ordinal() < TablePermission.Scope.OWN.ordinal()) {
        errors.add(new ValidationError(schema, table, "changeOwner requires update"));
      }
      if (p.share() && p.update().ordinal() < TablePermission.Scope.OWN.ordinal()) {
        errors.add(new ValidationError(schema, table, "share requires update"));
      }
    }
    return errors;
  }

  public void validateOrThrow() {
    List<ValidationError> errors = validate();
    if (!errors.isEmpty()) {
      String detail =
          errors.stream()
              .map(e -> "schema '" + e.schema() + "' table '" + e.table() + "': " + e.message())
              .collect(Collectors.joining("; "));
      throw new MolgenisException("Permission validation failed: " + detail);
    }
  }

  public TablePermission resolveFor(String schemaName, String tableName) {
    TablePermission wildcardBoth = entries.get(key("*", "*"));
    TablePermission wildcardSchema = entries.get(key("*", tableName));
    TablePermission wildcardTable = entries.get(key(schemaName, "*"));
    TablePermission exact = entries.get(key(schemaName, tableName));

    TablePermission.Select select = TablePermission.Select.NONE;
    TablePermission.Scope insert = TablePermission.Scope.NONE;
    TablePermission.Scope update = TablePermission.Scope.NONE;
    TablePermission.Scope delete = TablePermission.Scope.NONE;
    boolean changeOwner = false;
    boolean share = false;

    for (TablePermission p :
        new TablePermission[] {wildcardBoth, wildcardSchema, wildcardTable, exact}) {
      if (p == null) continue;
      if (p.select().permissivenessLevel() > select.permissivenessLevel()) select = p.select();
      if (p.insert().ordinal() > insert.ordinal()) insert = p.insert();
      if (p.update().ordinal() > update.ordinal()) update = p.update();
      if (p.delete().ordinal() > delete.ordinal()) delete = p.delete();
      if (p.changeOwner()) changeOwner = true;
      if (p.share()) share = true;
    }

    return new TablePermission(
        schemaName, tableName, select, insert, update, delete, changeOwner, share);
  }

  public record TableRef(String schema, String table) {}
}
