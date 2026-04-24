package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TablePermissionTest {

  @Test
  void wildcardAccepted() {
    TablePermission p =
        new TablePermission(
            "*",
            "*",
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            false,
            false);
    assertEquals("*", p.schema());
    assertEquals("*", p.table());
  }
}
