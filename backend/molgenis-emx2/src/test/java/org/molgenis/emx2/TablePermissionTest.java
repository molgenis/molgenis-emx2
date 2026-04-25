package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

public class TablePermissionTest {

  @Test
  void selectEnumOrderIsLeastToMostPermissive() {
    TablePermission.Select[] values = TablePermission.Select.values();

    assertEquals(TablePermission.Select.NONE, values[0], "NONE must be first (least permissive)");

    assertTrue(
        TablePermission.Select.NONE.permissivenessLevel()
            < TablePermission.Select.EXISTS.permissivenessLevel(),
        "NONE < EXISTS");
    assertTrue(
        TablePermission.Select.EXISTS.permissivenessLevel()
            < TablePermission.Select.COUNT.permissivenessLevel(),
        "EXISTS < COUNT");
    assertTrue(
        TablePermission.Select.COUNT.permissivenessLevel()
            < TablePermission.Select.RANGE.permissivenessLevel(),
        "COUNT < RANGE");
    assertTrue(
        TablePermission.Select.RANGE.permissivenessLevel()
            < TablePermission.Select.AGGREGATE.permissivenessLevel(),
        "RANGE < AGGREGATE");
    assertTrue(
        TablePermission.Select.AGGREGATE.permissivenessLevel()
            < TablePermission.Select.OWN.permissivenessLevel(),
        "AGGREGATE < OWN");
    assertTrue(
        TablePermission.Select.OWN.permissivenessLevel()
            < TablePermission.Select.GROUP.permissivenessLevel(),
        "OWN < GROUP");
    assertTrue(
        TablePermission.Select.GROUP.permissivenessLevel()
            < TablePermission.Select.ALL.permissivenessLevel(),
        "GROUP < ALL");
  }

  @Test
  void tablePermissionConstructorTakesSelectNotScopePlusViewMode() throws NoSuchMethodException {
    Method selectGetter = TablePermission.class.getMethod("select");
    assertEquals(
        TablePermission.Select.class,
        selectGetter.getReturnType(),
        "select() must return TablePermission.Select (unified enum)");

    assertThrows(
        NoSuchMethodException.class,
        () -> TablePermission.class.getMethod("viewMode"),
        "viewMode() getter must not exist after Story 3.4 collapse");

    TablePermission perm =
        new TablePermission(
            "s",
            "t",
            TablePermission.Select.AGGREGATE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false);
    assertEquals(TablePermission.Select.AGGREGATE, perm.select());
  }
}
