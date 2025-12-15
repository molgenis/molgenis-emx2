package org.molgenis.emx2;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaBasedPermissionsTest {

  @Test
  void whenManagerOrOwner_thenMembersAccessible() {
    var allowed = List.of(Privileges.OWNER, Privileges.MANAGER);
    var notAllowed = Arrays.stream(Privileges.values()).filter(not(allowed::contains)).toList();

    Schema schema = mock(Schema.class);
    var permissions = new SchemaBasedPermissions(schema);

    for (Privileges privilege : allowed) {
      when(schema.getInheritedRolesForActiveUser()).thenReturn(List.of(privilege.toString()));
      assertTrue(
          permissions.canAccessMembers(),
          "Expected " + privilege + " to be able to access members");
    }

    for (Privileges privilege : notAllowed) {
      when(schema.getInheritedRolesForActiveUser()).thenReturn(List.of(privilege.toString()));
      assertFalse(
          permissions.canAccessMembers(),
          "Expected " + privilege + " to not be able to access members");
    }
  }
}
