package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Privileges.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;

public class SqlPermissionEvaluator implements PermissionEvaluator {

  private final SqlSchemaMetadata schema;
  private Set<String> tablesWithSelectPermission;
  private List<TablePermission> activeUserPermissions;

  public SqlPermissionEvaluator(SqlSchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public boolean canView(TableMetadata table) {
    return table.getTableType() == TableType.ONTOLOGIES
        || hasRole(VIEWER)
        || tableHasSelectGrant(table.getTableName());
  }

  @Override
  public AggregateLevel getAggregateLevel(TableMetadata table) {
    if (canView(table)) return AggregateLevel.FULL;
    if (hasRole(COUNT)) return AggregateLevel.COUNT;
    if (hasRole(AGGREGATOR)) return AggregateLevel.AGGREGATOR;
    if (hasRole(RANGE)) return AggregateLevel.RANGE;
    if (hasRole(EXISTS)) return AggregateLevel.EXISTS;
    return AggregateLevel.NONE;
  }

  @Override
  public boolean canInsert(TableMetadata table) {
    return hasRole(EDITOR) || tableHasGrant(table.getTableName(), TablePermission::insert);
  }

  @Override
  public boolean canUpdate(TableMetadata table) {
    return hasRole(EDITOR) || tableHasGrant(table.getTableName(), TablePermission::update);
  }

  @Override
  public boolean canDelete(TableMetadata table) {
    return hasRole(EDITOR) || tableHasGrant(table.getTableName(), TablePermission::delete);
  }

  @Override
  public boolean canManage() {
    return isAdmin() || hasRole(MANAGER);
  }

  @Override
  public boolean isAdmin() {
    return schema.getDatabase().isAdmin();
  }

  private boolean hasRole(Privileges privilege) {
    return schema.hasActiveUserRole(privilege.toString());
  }

  private boolean tableHasSelectGrant(String tableName) {
    Set<String> perms = getTablesWithSelectPermission();
    return perms.contains("*") || perms.contains(tableName);
  }

  private boolean tableHasGrant(
      String tableName, java.util.function.Function<TablePermission, Boolean> grantExtractor) {
    return getActiveUserPermissions().stream()
        .anyMatch(
            p ->
                (p.table().equals("*") || p.table().equals(tableName))
                    && Boolean.TRUE.equals(grantExtractor.apply(p)));
  }

  private List<TablePermission> getActiveUserPermissions() {
    if (activeUserPermissions == null) {
      activeUserPermissions =
          schema.getDatabase().getRoleManager().getTablePermissionsForActiveUser(schema.getName());
    }
    return activeUserPermissions;
  }

  private Set<String> getTablesWithSelectPermission() {
    if (tablesWithSelectPermission == null) {
      tablesWithSelectPermission =
          getActiveUserPermissions().stream()
              .filter(p -> Boolean.TRUE.equals(p.select()))
              .map(TablePermission::table)
              .collect(Collectors.toUnmodifiableSet());
    }
    return tablesWithSelectPermission;
  }
}
