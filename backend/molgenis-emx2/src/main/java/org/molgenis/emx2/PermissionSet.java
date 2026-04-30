package org.molgenis.emx2;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

  public Stream<TablePermission> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  public List<ValidationError> validate() {
    List<ValidationError> errors = new ArrayList<>();
    for (TablePermission p : entries.values()) {
      String schema = p.schema();
      String table = p.table();
      boolean hasRowAccess = p.hasRowAccess();
      boolean hasAnySelect = p.hasAnySelect();
      if (p.delete() != TablePermission.UpdateScope.NONE && !hasAnySelect) {
        errors.add(new ValidationError(schema, table, "delete requires select"));
      }
      if (p.update() != TablePermission.UpdateScope.NONE && !hasAnySelect) {
        errors.add(new ValidationError(schema, table, "update requires select"));
      }
      if (p.changeOwner() && p.update().ordinal() < TablePermission.UpdateScope.OWN.ordinal()) {
        errors.add(
            new ValidationError(schema, table, "changeOwner requires update scope OWN or higher"));
      }
      if (p.changeGroup() && p.update().ordinal() < TablePermission.UpdateScope.OWN.ordinal()) {
        errors.add(
            new ValidationError(schema, table, "changeGroup requires update scope OWN or higher"));
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
    TablePermission wildcardSchema = entries.get(key(schemaName, "*"));
    TablePermission wildcardTable = entries.get(key("*", tableName));
    TablePermission exact = entries.get(key(schemaName, tableName));

    Set<TablePermission.SelectScope> select = EnumSet.noneOf(TablePermission.SelectScope.class);
    TablePermission.UpdateScope insert = TablePermission.UpdateScope.NONE;
    TablePermission.UpdateScope update = TablePermission.UpdateScope.NONE;
    TablePermission.UpdateScope delete = TablePermission.UpdateScope.NONE;
    boolean changeOwner = false;
    boolean changeGroup = false;

    for (TablePermission p :
        new TablePermission[] {wildcardBoth, wildcardSchema, wildcardTable, exact}) {
      if (p == null) continue;
      select.addAll(p.select());
      if (p.insert().ordinal() > insert.ordinal()) insert = p.insert();
      if (p.update().ordinal() > update.ordinal()) update = p.update();
      if (p.delete().ordinal() > delete.ordinal()) delete = p.delete();
      if (p.changeOwner()) changeOwner = true;
      if (p.changeGroup()) changeGroup = true;
    }

    return new TablePermission(schemaName, tableName)
        .select(select)
        .insert(insert)
        .update(update)
        .delete(delete)
        .setChangeOwner(changeOwner)
        .setChangeGroup(changeGroup);
  }

  public record TableRef(String schema, String table) {}
}
