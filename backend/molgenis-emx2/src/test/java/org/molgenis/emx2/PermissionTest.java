package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PermissionTest {

  @Test
  void wildcardAccepted() {
    Permission p =
        new Permission(
            "*",
            "*",
            Permission.ViewScope.ALL,
            Permission.EditScope.ALL,
            Permission.EditScope.ALL,
            Permission.EditScope.ALL,
            false,
            false);
    assertEquals("*", p.schema());
    assertEquals("*", p.table());
  }
}
