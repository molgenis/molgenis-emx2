package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
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

  @Test
  void givenRow_whenOverriddenWithOther_thenProvidedValuesWin() {
    Row existing = Row.row("id", 1, "name", "spike", "tags", new String[] {"old"});
    Row provided = Row.row("name", "pooky", "tags", new String[] {"new"});

    Row result = existing.overrideWith(provided, Set.of());

    assertEquals("pooky", result.getString("name"));
    assertArrayEquals(new String[] {"new"}, result.getStringArray("tags"));
  }

  @Test
  void givenRow_whenOverriddenWithOther_thenColumnsNotProvidedKeepTheirValue() {
    Row existing = Row.row("id", 1, "name", "spike", "category", "cat");
    Row provided = Row.row("name", "pooky");

    Row result = existing.overrideWith(provided, Set.of());

    assertEquals(1, result.getInteger("id"));
    assertEquals("cat", result.getString("category"));
    assertEquals(Set.of("id", "name", "category"), result.getColumnNames());
  }

  @Test
  void givenRow_whenOverriddenWithOther_thenExcludedColumnsAreNotOverridden() {
    Row existing = Row.row("id", 1, "name", "spike");
    Row provided = Row.row("id", 2, "name", "pooky");

    Row result = existing.overrideWith(provided, Set.of("id"));

    assertEquals(1, result.getInteger("id"));
    assertEquals("pooky", result.getString("name"));
  }

  @Test
  void givenRow_whenOverriddenWithOther_thenProvidedNullOverridesTheValue() {
    Row existing = Row.row("id", 1, "name", "spike");
    Row provided = Row.row("name", null);

    Row result = existing.overrideWith(provided, Set.of());

    assertNull(result.getString("name"));
    assertTrue(result.containsName("name"));
  }

  @Test
  void givenRow_whenOverriddenWithOther_thenNeitherRowIsModified() {
    Row existing = Row.row("id", 1, "name", "spike");
    Row provided = Row.row("name", "pooky", "category", "cat");

    Row result = existing.overrideWith(provided, Set.of());

    assertEquals("spike", existing.getString("name"));
    assertFalse(existing.containsName("category"));
    assertEquals(Set.of("name", "category"), provided.getColumnNames());
    assertEquals("cat", result.getString("category"));
  }

  @Test
  void givenRow_whenOverriddenWithEmptyRow_thenResultEqualsOriginalValues() {
    Row existing = Row.row("id", 1, "name", "spike");

    Row result = existing.overrideWith(new Row(), Set.of("id"));

    assertEquals(existing.getValueMap(), result.getValueMap());
  }
}
