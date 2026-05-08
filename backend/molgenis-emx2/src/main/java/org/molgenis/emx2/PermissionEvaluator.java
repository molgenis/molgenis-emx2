package org.molgenis.emx2;

import static org.molgenis.emx2.Privileges.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PermissionEvaluator {

  private PermissionEvaluator() {}

  public static boolean canView(Schema schema, TableMetadata table) {
    return table.getTableType() == TableType.ONTOLOGIES
        || hasRole(schema, VIEWER)
        || permissionFor(schema, table).map(TablePermission::hasSelect).orElse(false);
  }

  public static AggregateLevel getAggregateLevel(Schema schema, TableMetadata table) {
    if (canView(schema, table)) return AggregateLevel.COUNT;
    if (hasRole(schema, COUNT)) return AggregateLevel.COUNT;
    if (hasRole(schema, AGGREGATOR)) return AggregateLevel.AGGREGATOR;
    if (hasRole(schema, RANGE)) return AggregateLevel.RANGE;
    if (hasRole(schema, EXISTS)) return AggregateLevel.EXISTS;
    return AggregateLevel.NONE;
  }

  public static boolean canCount(Schema schema, TableMetadata table) {
    return tablePermissionAtLeast(schema, table, AggregateLevel.COUNT);
  }

  public static boolean canRange(Schema schema, TableMetadata table) {
    return tablePermissionAtLeast(schema, table, AggregateLevel.RANGE);
  }

  public static boolean canExists(Schema schema, TableMetadata table) {
    return tablePermissionAtLeast(schema, table, AggregateLevel.EXISTS);
  }

  private static boolean tablePermissionAtLeast(
      Schema schema, TableMetadata table, AggregateLevel level) {
    return getAggregateLevel(schema, table).isAtLeast(level);
  }

  public static boolean canInsert(Schema schema, TableMetadata table) {
    return hasRole(schema, EDITOR)
        || permissionFor(schema, table).map(TablePermission::hasInsert).orElse(false);
  }

  public static boolean canUpdate(Schema schema, TableMetadata table) {
    return hasRole(schema, EDITOR)
        || permissionFor(schema, table).map(TablePermission::hasUpdate).orElse(false);
  }

  public static boolean canDelete(Schema schema, TableMetadata table) {
    return hasRole(schema, EDITOR)
        || permissionFor(schema, table).map(TablePermission::hasDelete).orElse(false);
  }

  public static boolean canManage(Schema schema) {
    return isAdmin(schema) || hasRole(schema, MANAGER);
  }

  public static boolean isAdmin(Schema schema) {
    return schema.getDatabase().isAdmin();
  }

  private static boolean hasRole(Schema schema, Privileges privilege) {
    return schema.getInheritedRolesForActiveUser().contains(privilege.toString());
  }

  private static Optional<TablePermission> permissionFor(Schema schema, TableMetadata table) {
    return Optional.ofNullable(getPermissionsByTable(schema).get(table.getTableName()));
  }

  private static Map<String, TablePermission> getPermissionsByTable(Schema schema) {
    return schema.getPermissionsForActiveUser().stream()
        .collect(Collectors.toUnmodifiableMap(TablePermission::table, p -> p));
  }
}
