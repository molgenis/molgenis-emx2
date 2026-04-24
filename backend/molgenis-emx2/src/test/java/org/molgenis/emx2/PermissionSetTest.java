package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class PermissionSetTest {

  @Test
  void putReplacesByKey() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    assertEquals(1, set.size());
    TablePermission resolved = set.resolveFor("s", "t");
    assertEquals(TablePermission.Scope.NONE, resolved.select());
  }

  @Test
  void validateReturnsAllErrors() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.ALL,
            true,
            true));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertTrue(errors.size() >= 2, "Expected at least 2 errors, got: " + errors.size());
  }

  @Test
  void deleteRequiresRead() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.OWN,
            false,
            false));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("delete") && msg.contains("read"), "Expected delete+read in: " + msg);
  }

  @Test
  void updateRequiresRead() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            false,
            false));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("update") && msg.contains("read"), "Expected update+read in: " + msg);
  }

  @Test
  void changeOwnerRequiresUpdate() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            true,
            false));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(
        msg.contains("changeowner") || msg.contains("change_owner") || msg.contains("update"),
        "Expected changeOwner+update in: " + msg);
  }

  @Test
  void shareRequiresUpdate() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            true));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("share") || msg.contains("update"), "Expected share+update in: " + msg);
  }

  @Test
  void serverRejectsInsteadOfUpgrading() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.OWN,
            false,
            false));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertFalse(errors.isEmpty(), "Validation should reject, not auto-fix");
    assertEquals(1, set.size(), "Set must not be mutated by validate()");
  }

  @Test
  void resolveForUnionPermissive() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission(
            "*",
            "*",
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            true,
            false));
    set.put(
        new TablePermission(
            "s",
            "t",
            TablePermission.Scope.GROUP,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    TablePermission resolved = set.resolveFor("s", "t");
    assertEquals(TablePermission.Scope.GROUP, resolved.select(), "GROUP > OWN, should pick GROUP");
    assertTrue(resolved.changeOwner(), "Wildcard changeOwner=true should OR into resolved");
  }

  @Test
  void schemaScopeDeferred() {
    assertFalse(
        java.util.Arrays.stream(TablePermission.Scope.values())
            .anyMatch(s -> s.name().equals("SCHEMA")),
        "Scope.SCHEMA must not exist in v1");
  }
}
