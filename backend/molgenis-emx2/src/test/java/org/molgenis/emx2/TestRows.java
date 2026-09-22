package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestRows {

  @Test
  void givenRow_ifValueMapIsEmpty_thenRowIsEmpty() {
    Row row = new Row();
    assertTrue(row.isEmpty());

    row.set("foo", "bar");
    assertFalse(row.isEmpty());
  }

  @Test
  void givenRowWithValueMap_ifAllValuesNull_thenRowIsEmpty() {
    Row row = Row.row("foo", null, "bar", null);
    assertTrue(row.isEmpty());

    row.set("foo", "test");
    assertFalse(row.isEmpty());
  }

  @Test
  void givenRow_thenInputShouldBeKeyValueWithStringKey() {

    try {
      new Row(1, 2);
      fail("should fail because column names must be string");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    try {
      new Row(true, 2);
      fail("should fail because column names must be string");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    try {
      new Row(true, 2, 3);
      fail("should fail because name value pairs must be even number of parameters");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    new Row("col1", 1, "col2", 2);
  }
}
