package org.molgenis.emx2;

import java.util.List;

public interface RoleManager {

  int MAX_ROLE_NAME_LENGTH = 32;

  void createRole(String name, String description);

  void deleteRole(String name);

  List<Role> listRoles();

  void grantRoleToUser(String role, String user);

  void revokeRoleFromUser(String role, String user);

  void setPermissions(String role, PermissionSet permissions);

  PermissionSet getPermissions(String role);

  PermissionSet getPermissionsForActiveUser();
}
