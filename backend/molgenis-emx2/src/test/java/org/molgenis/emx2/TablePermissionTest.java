package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class TablePermissionTest {

  @Test
  void selectEnumOrderIsLeastToMostPermissive() {
    TablePermission.SelectScope[] values = TablePermission.SelectScope.values();

    assertEquals(
        TablePermission.SelectScope.NONE, values[0], "NONE must be first (least permissive)");

    assertTrue(
        TablePermission.SelectScope.NONE.permissivenessLevel()
            < TablePermission.SelectScope.EXISTS.permissivenessLevel(),
        "NONE < EXISTS");
    assertTrue(
        TablePermission.SelectScope.EXISTS.permissivenessLevel()
            < TablePermission.SelectScope.COUNT.permissivenessLevel(),
        "EXISTS < COUNT");
    assertTrue(
        TablePermission.SelectScope.COUNT.permissivenessLevel()
            < TablePermission.SelectScope.RANGE.permissivenessLevel(),
        "COUNT < RANGE");
    assertTrue(
        TablePermission.SelectScope.RANGE.permissivenessLevel()
            < TablePermission.SelectScope.AGGREGATE.permissivenessLevel(),
        "RANGE < AGGREGATE");
    assertTrue(
        TablePermission.SelectScope.AGGREGATE.permissivenessLevel()
            < TablePermission.SelectScope.OWN.permissivenessLevel(),
        "AGGREGATE < OWN");
    assertTrue(
        TablePermission.SelectScope.OWN.permissivenessLevel()
            < TablePermission.SelectScope.GROUP.permissivenessLevel(),
        "OWN < GROUP");
    assertTrue(
        TablePermission.SelectScope.GROUP.permissivenessLevel()
            < TablePermission.SelectScope.ALL.permissivenessLevel(),
        "GROUP < ALL");
  }

  @Test
  void selectFieldIsSetOfSelectScope() {
    TablePermission perm =
        new TablePermission("s", "t").select(TablePermission.SelectScope.AGGREGATE);
    assertTrue(
        perm.select().contains(TablePermission.SelectScope.AGGREGATE),
        "select set must contain AGGREGATE");
    assertEquals(1, perm.select().size(), "singleton select should have exactly one member");
  }

  @Test
  void emptySelectMeansNoAccess() {
    TablePermission perm = new TablePermission("s", "t");
    assertTrue(perm.select().isEmpty(), "empty select set means no read access");
    assertFalse(perm.hasAnySelect(), "hasAnySelect must be false for empty set");
    assertFalse(perm.hasRowAccess(), "hasRowAccess must be false for empty set");
  }

  @Test
  void multiValueSelectSet() {
    Set<TablePermission.SelectScope> both =
        java.util.EnumSet.of(TablePermission.SelectScope.OWN, TablePermission.SelectScope.COUNT);
    TablePermission perm = new TablePermission("s", "t").select(both);
    assertTrue(perm.select().contains(TablePermission.SelectScope.OWN));
    assertTrue(perm.select().contains(TablePermission.SelectScope.COUNT));
    assertEquals(2, perm.select().size());
    assertTrue(perm.hasRowAccess(), "OWN in set means row access is granted");
    assertTrue(perm.hasAnySelect(), "non-empty set means hasAnySelect");
  }

  @Test
  void allowsExactCountExcludesRange() {
    assertTrue(TablePermission.SelectScope.ALL.allowsExactCount());
    assertTrue(TablePermission.SelectScope.OWN.allowsExactCount());
    assertTrue(TablePermission.SelectScope.GROUP.allowsExactCount());
    assertTrue(TablePermission.SelectScope.COUNT.allowsExactCount());
    assertTrue(TablePermission.SelectScope.AGGREGATE.allowsExactCount());
    assertFalse(
        TablePermission.SelectScope.RANGE.allowsExactCount(), "RANGE must not allow exact count");
    assertFalse(TablePermission.SelectScope.EXISTS.allowsExactCount());
    assertFalse(TablePermission.SelectScope.NONE.allowsExactCount());
  }

  @Test
  void updateScopeAllowsOwnRows() {
    assertFalse(TablePermission.UpdateScope.NONE.allowsOwnRows());
    assertTrue(TablePermission.UpdateScope.OWN.allowsOwnRows());
    assertTrue(TablePermission.UpdateScope.GROUP.allowsOwnRows());
    assertTrue(TablePermission.UpdateScope.ALL.allowsOwnRows());
  }

  @Test
  void updateScopeAllowsGroupRows() {
    assertFalse(TablePermission.UpdateScope.NONE.allowsGroupRows());
    assertFalse(TablePermission.UpdateScope.OWN.allowsGroupRows());
    assertTrue(TablePermission.UpdateScope.GROUP.allowsGroupRows());
    assertTrue(TablePermission.UpdateScope.ALL.allowsGroupRows());
  }

  @Test
  void updateScopeAllowsAllRows() {
    assertFalse(TablePermission.UpdateScope.NONE.allowsAllRows());
    assertFalse(TablePermission.UpdateScope.OWN.allowsAllRows());
    assertFalse(TablePermission.UpdateScope.GROUP.allowsAllRows());
    assertTrue(TablePermission.UpdateScope.ALL.allowsAllRows());
  }

  @Test
  void nullOrEmptySelectCoercedToEmptySet() {
    TablePermission withNull =
        new TablePermission("s", "t").select((Set<TablePermission.SelectScope>) null);
    assertTrue(withNull.select().isEmpty(), "null select coerced to empty set");
  }
}
