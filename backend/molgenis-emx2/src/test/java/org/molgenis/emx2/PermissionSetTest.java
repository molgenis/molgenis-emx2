package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class PermissionSetTest {

  private static final java.util.function.Function<PermissionSet.TableRef, Boolean> RLS_DISABLED =
      ref -> false;
  private static final java.util.function.Function<PermissionSet.TableRef, Boolean> RLS_ENABLED =
      ref -> true;

  @Test
  void putReplacesByKey() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.ALL,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            false));
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            false));
    assertEquals(1, set.size());
    Permission resolved = set.resolveFor("s", "t");
    assertEquals(Permission.ViewScope.NONE, resolved.select());
  }

  @Test
  void validateReturnsAllErrors() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.ALL,
            true,
            true));
    List<PermissionSet.ValidationError> errors = set.validate(RLS_DISABLED);
    assertTrue(errors.size() >= 2, "Expected at least 2 errors, got: " + errors.size());
  }

  @Test
  void deleteRequiresRead() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.OWN,
            false,
            false));
    List<PermissionSet.ValidationError> errors = set.validate(RLS_ENABLED);
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("delete") && msg.contains("read"), "Expected delete+read in: " + msg);
  }

  @Test
  void updateRequiresRead() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.OWN,
            Permission.EditScope.NONE,
            false,
            false));
    List<PermissionSet.ValidationError> errors = set.validate(RLS_ENABLED);
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("update") && msg.contains("read"), "Expected update+read in: " + msg);
  }

  @Test
  void changeOwnerRequiresUpdate() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.ALL,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            true,
            false));
    List<PermissionSet.ValidationError> errors = set.validate(RLS_ENABLED);
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
        new Permission(
            "s",
            "t",
            Permission.ViewScope.ALL,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            true));
    List<PermissionSet.ValidationError> errors = set.validate(RLS_ENABLED);
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("share") || msg.contains("update"), "Expected share+update in: " + msg);
  }

  @Test
  void serverRejectsInsteadOfUpgrading() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.OWN,
            false,
            false));
    List<PermissionSet.ValidationError> errors = set.validate(RLS_ENABLED);
    assertFalse(errors.isEmpty(), "Validation should reject, not auto-fix");
    assertEquals(1, set.size(), "Set must not be mutated by validate()");
  }

  @Test
  void resolveForUnionPermissive() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "*",
            "*",
            Permission.ViewScope.OWN,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            true,
            false));
    set.put(
        new Permission(
            "s",
            "t",
            Permission.ViewScope.GROUP,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            false));
    Permission resolved = set.resolveFor("s", "t");
    assertEquals(Permission.ViewScope.GROUP, resolved.select(), "GROUP > OWN, should pick GROUP");
    assertTrue(resolved.changeOwner(), "Wildcard changeOwner=true should OR into resolved");
  }

  @Test
  void ownGroupRequiresRlsFlag() {
    PermissionSet set = new PermissionSet();
    set.put(
        new Permission(
            "s1",
            "t1",
            Permission.ViewScope.OWN,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            false));

    List<PermissionSet.ValidationError> errorsWithout = set.validate(RLS_DISABLED);
    assertFalse(errorsWithout.isEmpty(), "OWN scope without RLS should produce errors");
    String msg = errorsWithout.get(0).message().toLowerCase();
    assertTrue(
        msg.contains("rls") || msg.contains("row_level_security") || msg.contains("row level"),
        "Error should mention row level security, got: " + msg);

    List<PermissionSet.ValidationError> errorsWith = set.validate(RLS_ENABLED);
    assertTrue(errorsWith.isEmpty(), "OWN scope with RLS enabled should produce no errors");
  }

  @Test
  void schemaScopeDeferred() {
    assertFalse(
        java.util.Arrays.stream(Permission.ViewScope.values())
            .anyMatch(s -> s.name().equals("SCHEMA")),
        "ViewScope.SCHEMA must not exist in v1");
  }
}
