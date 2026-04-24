package org.molgenis.emx2;

import java.util.List;

public interface RoleManager {

  void createRole(String name, String description);

  void deleteRole(String name);

  List<Role> listRoles();

  void grantRoleToUser(String role, String user);

  void revokeRoleFromUser(String role, String user);

  void setPermissions(String role, PermissionSet permissions);

  PermissionSet getPermissions(String role);

  PermissionSet getPermissionsForActiveUser();
}
