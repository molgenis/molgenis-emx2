package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestNameValidation {
  @Test
  public void testNameValidation() {
    assertThrows(
        MolgenisException.class,
        () -> new Column("first name"),
        "should fail because ' ' is not allowed");
    assertThrows(
        MolgenisException.class,
        () -> new TableMetadata("first name"),
        "should fail because ' ' is not allowed");
    assertThrows(
        MolgenisException.class,
        () -> new SchemaMetadata("first name"),
        "should fail because ' ' is not allowed");
  }
}
