package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestPrivileges {

  @ParameterizedTest
  @ValueSource(
      strings = {"Viewer", "Editor", "Manager", "Owner", "Exists", "Range", "Aggregator", "Count"})
  void isSystemRoleRecognisesAllBuiltInRoles(String roleName) {
    assertTrue(Privileges.isSystemRole(roleName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"CustomRole", "viewer", "VIEWER", ""})
  void isSystemRoleRejectsCustomAndMalformedNames(String roleName) {
    assertFalse(Privileges.isSystemRole(roleName));
  }
}
