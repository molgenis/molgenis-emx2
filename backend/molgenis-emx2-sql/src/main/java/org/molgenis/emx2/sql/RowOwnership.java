package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Constants.MG_ROLES;

import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionEvaluator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

class RowOwnership {

  private final Schema schema;
  private final TableMetadata table;
  private List<String> rolesInSchema;

  RowOwnership(Schema schema, TableMetadata table) {
    this.schema = schema;
    this.table = table;
  }

  void validateAndAssignOwnerWhenOmitted(Iterable<Row> rows) {
    if (!hasMgRoleColumn()) return;
    apply(rows, defaultRoleForActiveUser());
  }

  void validateOwners(Iterable<Row> rows) {
    if (!hasMgRoleColumn()) return;
    apply(rows, null);
  }

  private void apply(Iterable<Row> rows, String defaultRole) {
    for (Row row : rows) {
      String owner = ownerOf(row);
      if (owner == null) {
        if (defaultRole != null) {
          row.set(MG_ROLES, new String[] {defaultRole});
        }
      } else {
        validateRoleIsAssignable(owner);
        if (PermissionEvaluator.isRowLevelRestricted(schema, table)) validateUserMayAssign(owner);
      }
    }
  }

  private boolean hasMgRoleColumn() {
    return table.getColumn(MG_ROLES) != null;
  }

  private static String ownerOf(Row row) {
    String[] mgRoles = row.getStringArray(MG_ROLES);
    if (mgRoles == null || mgRoles.length == 0) return null;
    if (mgRoles.length > 1) {
      throw new MolgenisException(
          "mg_roles can only contain a single role, multiple were provided: "
              + Arrays.toString(mgRoles));
    }
    return mgRoles[0];
  }

  private void validateRoleIsAssignable(String role) {
    if (!SqlRoleManager.isUserAssignableRole(role)) {
      throw new MolgenisException(
          "mg_roles value '"
              + role
              + "' is an internal or system role and cannot be assigned as row owner");
    }
    if (!rolesInSchema().contains(role)) {
      throw new MolgenisException(
          "mg_roles value '"
              + role
              + "' is not a valid custom role in schema '"
              + table.getSchemaName()
              + "'");
    }
  }

  private void validateUserMayAssign(String role) {
    if (!schema.getInheritedRolesForActiveUser().contains(role)) {
      throw new MolgenisException(
          "Permission denied: you must be Manager or hold the role '" + role + "' to set mg_roles");
    }
  }

  private String defaultRoleForActiveUser() {
    if (!PermissionEvaluator.isRowLevelRestricted(schema, table)) return null;
    List<String> roles =
        schema.getInheritedRolesForActiveUser().stream()
            .filter(SqlRoleManager::isUserAssignableRole)
            .toList();
    if (roles.isEmpty()) return null;
    if (roles.size() > 1) {
      throw new MolgenisException("Cannot determine row owner: multiple roles found: " + roles);
    }
    return roles.getFirst();
  }

  private List<String> rolesInSchema() {
    if (rolesInSchema == null) rolesInSchema = schema.getRoles();
    return rolesInSchema;
  }
}
