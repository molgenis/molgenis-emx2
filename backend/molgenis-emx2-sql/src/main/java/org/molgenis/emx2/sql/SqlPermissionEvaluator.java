package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Privileges.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;

public class SqlPermissionEvaluator implements PermissionEvaluator {

  private final SqlSchemaMetadata schema;
  private Map<String, TablePermission> permissionsByTable;

  public SqlPermissionEvaluator(SqlSchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public boolean canView(TableMetadata table) {
    return table.getTableType() == TableType.ONTOLOGIES
        || hasRole(VIEWER)
        || permissionFor(table).map(TablePermission::hasSelect).orElse(false);
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
    return hasRole(EDITOR) || permissionFor(table).map(TablePermission::hasInsert).orElse(false);
  }

  @Override
  public boolean canUpdate(TableMetadata table) {
    return hasRole(EDITOR) || permissionFor(table).map(TablePermission::hasUpdate).orElse(false);
  }

  @Override
  public boolean canDelete(TableMetadata table) {
    return hasRole(EDITOR) || permissionFor(table).map(TablePermission::hasDelete).orElse(false);
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

  private Optional<TablePermission> permissionFor(TableMetadata table) {
    return Optional.ofNullable(getPermissionsByTable().get(table.getTableName()));
  }

  private Map<String, TablePermission> getPermissionsByTable() {
    if (permissionsByTable == null) {
      permissionsByTable =
          schema
              .getDatabase()
              .getRoleManager()
              .getTablePermissionsForActiveUser(schema.getName())
              .stream()
              .collect(Collectors.toUnmodifiableMap(TablePermission::table, p -> p));
    }
    return permissionsByTable;
  }
}
