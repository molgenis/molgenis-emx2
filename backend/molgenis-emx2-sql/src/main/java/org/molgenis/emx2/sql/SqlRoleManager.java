package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Constants.*;

import java.util.List;
import org.molgenis.emx2.MolgenisException;

public class SqlRoleManager {
  static final List<String> SYSTEM_ROLES =
      List.of(
          ROLE_EXISTS,
          ROLE_RANGE,
          ROLE_AGGREGATOR,
          ROLE_COUNT,
          ROLE_VIEWER,
          ROLE_EDITOR,
          ROLE_MANAGER,
          ROLE_OWNER);

  private final SqlDatabase database;

  public SqlRoleManager(SqlDatabase database) {
    this.database = database;
  }

  public void createRole(String schemaName, String roleName, boolean isRowLevel) {
    throw new UnsupportedOperationException("SqlRoleManager.createRole not yet implemented");
  }

  public void deleteRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot delete system role: " + roleName);
    }
    throw new UnsupportedOperationException("SqlRoleManager.deleteRole not yet implemented");
  }

  public boolean roleExists(String schemaName, String roleName) {
    throw new UnsupportedOperationException("SqlRoleManager.roleExists not yet implemented");
  }

  public boolean isRowLevel(String schemaName, String roleName) {
    throw new UnsupportedOperationException("SqlRoleManager.isRowLevel not yet implemented");
  }

  public void addMember(String schemaName, String roleName, String userName) {
    throw new UnsupportedOperationException("SqlRoleManager.addMember not yet implemented");
  }

  public void removeMember(String schemaName, String roleName, String userName) {
    throw new UnsupportedOperationException("SqlRoleManager.removeMember not yet implemented");
  }

  public void grantTablePermission(
      String schemaName, String roleName, String tableName, String privilege) {
    throw new UnsupportedOperationException(
        "SqlRoleManager.grantTablePermission not yet implemented");
  }

  public void revokeTablePermission(
      String schemaName, String roleName, String tableName, String privilege) {
    throw new UnsupportedOperationException(
        "SqlRoleManager.revokeTablePermission not yet implemented");
  }

  public void setDescription(String schemaName, String roleName, String description) {
    throw new UnsupportedOperationException("SqlRoleManager.setDescription not yet implemented");
  }

  public String getDescription(String schemaName, String roleName) {
    throw new UnsupportedOperationException("SqlRoleManager.getDescription not yet implemented");
  }

  public List<String> getRolesForSchema(String schemaName) {
    throw new UnsupportedOperationException("SqlRoleManager.getRolesForSchema not yet implemented");
  }

  public boolean isSystemRole(String roleName) {
    return SYSTEM_ROLES.contains(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }
}
