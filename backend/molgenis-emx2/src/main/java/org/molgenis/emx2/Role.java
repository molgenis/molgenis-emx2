package org.molgenis.emx2;

import java.util.List;

public class Role {
  private String roleName;

  private String description;
  private boolean systemRole = false;
  private List<TablePermission> permissions = List.of();

  public Role() {}

  public Role(String roleName) {
    this.roleName = roleName;
  }

  public String getRoleName() {
    return roleName;
  }

  public Role setRoleName(String roleName) {
    this.roleName = roleName;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Role setDescription(String description) {
    this.description = description;
    return this;
  }

  public boolean isSystemRole() {
    return systemRole;
  }

  public Role setSystemRole(boolean systemRole) {
    this.systemRole = systemRole;
    return this;
  }

  public Role withPermissions(List<TablePermission> permissions) {
    this.permissions = permissions != null ? permissions : List.of();
    return this;
  }

  public String name() {
    return roleName;
  }

  public List<TablePermission> permissions() {
    return permissions;
  }
}
