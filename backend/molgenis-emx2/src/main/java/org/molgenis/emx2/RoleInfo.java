package org.molgenis.emx2;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleInfo {
  private String name;
  private String description;
  private boolean system;
  private List<Permission> permissions = new ArrayList<>();

  public RoleInfo() {}

  public RoleInfo(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public RoleInfo setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RoleInfo setDescription(String description) {
    this.description = description;
    return this;
  }

  public boolean isSystem() {
    return system;
  }

  public RoleInfo setSystem(boolean system) {
    this.system = system;
    return this;
  }

  public List<Permission> getPermissions() {
    return permissions;
  }

  public RoleInfo setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
    return this;
  }

  public RoleInfo addPermission(Permission permission) {
    this.permissions.add(permission);
    return this;
  }
}
