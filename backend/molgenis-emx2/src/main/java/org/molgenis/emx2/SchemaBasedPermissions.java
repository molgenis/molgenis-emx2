package org.molgenis.emx2;

import java.util.List;

public class SchemaBasedPermissions implements Permissions {

  private final Schema schema;

  public SchemaBasedPermissions(Schema schema) {
    this.schema = schema;
  }

  @Override
  public boolean canAccessMembers() {
    List<String> roles = schema.getInheritedRolesForActiveUser();
    return roles.contains(Privileges.MANAGER.toString())
        || roles.contains(Privileges.OWNER.toString());
  }
}
