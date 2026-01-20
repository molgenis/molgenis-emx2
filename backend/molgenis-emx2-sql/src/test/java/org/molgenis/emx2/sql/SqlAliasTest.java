package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqlAliasTest {

  @Test
  void shouldLayerAlias() {
    SqlAlias a1 = SqlAlias.withAlias("a1");
    SqlAlias a2 = SqlAlias.withAlias("a2", a1);
    SqlAlias a3 = SqlAlias.withAlias("a3", a2);

    assertTrue(a1.getParent().isEmpty());
    assertTrue(a1.getTopParent().isEmpty());

    assertEquals(a1, a2.getParent().orElseThrow());
    assertEquals(a1, a2.getTopParent().orElseThrow());

    assertEquals(a2, a3.getParent().orElseThrow());
    assertEquals(a1, a3.getTopParent().orElseThrow());
  }

  @Test
  void shouldGetQualifiedName() {
    SqlAlias a1 = SqlAlias.withAlias("a1");
    SqlAlias a2 = SqlAlias.withAlias("a2", a1);
    SqlAlias a3 = SqlAlias.withAlias("a3", a2);

    assertEquals("a1", a1.getQualifiedName());
    assertEquals("a1-a2", a2.getQualifiedName());
    assertEquals("a1-a2-a3", a3.getQualifiedName());
  }

  @Test
  void givenAnyParentThatAllowsAlias_thenAllowAlias() {
    SqlAlias a1 = SqlAlias.withAlias("a1");
    SqlAlias a2 = SqlAlias.withoutAlias("a2", a1);
    SqlAlias a3 = SqlAlias.withoutAlias("a3", a2);

    assertTrue(a1.allowsAlias());
    assertTrue(a2.allowsAlias());
    assertTrue(a3.allowsAlias());
  }

  @Test
  void givenAllParentsDisallowAlias_thenDontAllowAlias() {
    SqlAlias a1 = SqlAlias.withoutAlias("a1");
    SqlAlias a2 = SqlAlias.withoutAlias("a2", a1);
    SqlAlias a3 = SqlAlias.withoutAlias("a3", a2);

    assertFalse(a1.allowsAlias());
    assertFalse(a2.allowsAlias());
    assertFalse(a3.allowsAlias());
  }

  @Test
  void givenNoParents_thenAllowAliasBasedOnLeaf() {
    assertTrue(SqlAlias.withAlias("allows").allowsAlias());
    assertFalse(SqlAlias.withoutAlias("disallows").allowsAlias());
  }
}
