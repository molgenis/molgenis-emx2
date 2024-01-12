package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestColumn {
  @Test
  public void isSystemColumn() {
    assertFalse(new Column("test").isSystemColumn());
    assertTrue(new Column("mg_test").isSystemColumn());
    assertFalse(new Column("MG_test").isSystemColumn());
  }
}
