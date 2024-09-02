package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestColumn {
  @Test
  public void isSystemColumn() {
    assertAll(
        () -> assertFalse(new Column("test").isSystemColumn()),
        () -> assertTrue(new Column("mg_test").isSystemColumn()),
        () -> assertFalse(new Column("MG_test").isSystemColumn()));
  }

  @Test
  public void validColumnName() {
    assertAll(
        () -> assertThrows(MolgenisException.class, () -> new Column("%")), // invalid character
        () -> assertDoesNotThrow(() -> new Column("a")), // 1 or more legal characters
        () -> assertThrows(MolgenisException.class, () -> new Column("a_ b")), // underscore + space
        () -> assertThrows(MolgenisException.class, () -> new Column("a _b")), // space + underscore
        () -> assertDoesNotThrow(() -> new Column("a_b c"))); // valid underscore & space
  }
}
