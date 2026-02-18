package org.molgenis.emx2;

import java.util.List;

public record RoleInfo(
    String name, String description, boolean system, List<Permission> permissions) {

  public RoleInfo(String name) {
    this(name, null, false, List.of());
  }

  public RoleInfo {
    permissions = permissions == null ? List.of() : List.copyOf(permissions);
  }
}
