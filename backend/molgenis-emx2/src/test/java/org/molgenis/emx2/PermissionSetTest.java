package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PermissionSetTest {

  @Test
  void putReplacesByKey() {
    PermissionSet set = new PermissionSet();
    set.put(new TablePermission("s", "t").select(TablePermission.SelectScope.ALL));
    set.put(new TablePermission("s", "t"));
    assertEquals(1, set.size());
    TablePermission resolved = set.resolveFor("s", "t");
    assertTrue(resolved.select().isEmpty(), "select should be empty after replace");
  }

  @Test
  void validateReturnsAllErrors() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission("s", "t")
            .delete(TablePermission.UpdateScope.ALL)
            .setChangeOwner(true)
            .setChangeGroup(true));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertTrue(errors.size() >= 2, "Expected at least 2 errors, got: " + errors.size());
  }

  @Test
  void deleteRequiresRead() {
    PermissionSet set = new PermissionSet();
    set.put(new TablePermission("s", "t").delete(TablePermission.UpdateScope.OWN));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("delete") && msg.contains("read"), "Expected delete+read in: " + msg);
  }

  @Test
  void updateRequiresRead() {
    PermissionSet set = new PermissionSet();
    set.put(new TablePermission("s", "t").update(TablePermission.UpdateScope.OWN));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(msg.contains("update") && msg.contains("read"), "Expected update+read in: " + msg);
  }

  @Test
  void changeOwnerRequiresUpdate() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission("s", "t").select(TablePermission.SelectScope.ALL).setChangeOwner(true));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(
        msg.contains("changeowner") || msg.contains("change_owner") || msg.contains("update"),
        "Expected changeOwner+update in: " + msg);
  }

  @Test
  void changeGroupRequiresUpdate() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission("s", "t").select(TablePermission.SelectScope.ALL).setChangeGroup(true));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertEquals(1, errors.size());
    String msg = errors.get(0).message().toLowerCase();
    assertTrue(
        msg.contains("changegroup") || msg.contains("update"),
        "Expected changeGroup+update in: " + msg);
  }

  @Test
  void serverRejectsInsteadOfUpgrading() {
    PermissionSet set = new PermissionSet();
    set.put(new TablePermission("s", "t").delete(TablePermission.UpdateScope.OWN));
    List<PermissionSet.ValidationError> errors = set.validate();
    assertFalse(errors.isEmpty(), "Validation should reject, not auto-fix");
    assertEquals(1, set.size(), "Set must not be mutated by validate()");
  }

  @Test
  void resolveForUnionPermissive() {
    PermissionSet set = new PermissionSet();
    set.put(
        new TablePermission("*", "*").select(TablePermission.SelectScope.OWN).setChangeOwner(true));
    set.put(new TablePermission("s", "t").select(TablePermission.SelectScope.GROUP));
    TablePermission resolved = set.resolveFor("s", "t");
    assertTrue(
        resolved.select().contains(TablePermission.SelectScope.GROUP),
        "resolved set must contain GROUP");
    assertTrue(
        resolved.select().contains(TablePermission.SelectScope.OWN),
        "resolved set must contain OWN (from wildcard)");
    assertTrue(resolved.changeOwner(), "Wildcard changeOwner=true should OR into resolved");
  }

  @Test
  void schemaScopeDeferred() {
    assertFalse(
        java.util.Arrays.stream(TablePermission.UpdateScope.values())
            .anyMatch(s -> s.name().equals("SCHEMA")),
        "UpdateScope.SCHEMA must not exist in v1");
  }

  @Test
  void unionPicksMostPermissiveSelect() {
    PermissionSet set = new PermissionSet();
    set.put(new TablePermission("s", "t").select(TablePermission.SelectScope.AGGREGATE));
    set.put(new TablePermission("*", "*").select(TablePermission.SelectScope.ALL));
    TablePermission resolved = set.resolveFor("s", "t");
    assertTrue(
        resolved.select().contains(TablePermission.SelectScope.ALL),
        "ALL must be in the union set");
    assertTrue(
        resolved.select().contains(TablePermission.SelectScope.AGGREGATE),
        "AGGREGATE must also be in the union set");
  }

  @Test
  void resolveForUnionsSelectSets() {
    Set<TablePermission.SelectScope> ownCount =
        EnumSet.of(TablePermission.SelectScope.OWN, TablePermission.SelectScope.COUNT);
    PermissionSet set = new PermissionSet();
    set.put(new TablePermission("s", "t").select(ownCount));
    set.put(new TablePermission("*", "*").select(TablePermission.SelectScope.AGGREGATE));
    TablePermission resolved = set.resolveFor("s", "t");
    assertTrue(resolved.select().contains(TablePermission.SelectScope.OWN));
    assertTrue(resolved.select().contains(TablePermission.SelectScope.COUNT));
    assertTrue(resolved.select().contains(TablePermission.SelectScope.AGGREGATE));
    assertEquals(3, resolved.select().size());
  }
}
