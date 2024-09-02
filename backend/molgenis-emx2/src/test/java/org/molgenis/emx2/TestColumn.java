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
        // valid: 1 or more legal characters
        () -> assertDoesNotThrow(() -> new Column("a")),
        // valid: a space
        () -> assertDoesNotThrow(() -> new Column("first name")),
        // valid: space & underscore but not next to each other
        () -> assertDoesNotThrow(() -> new Column("yet_another name")),
        // invalid: # should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("#first name")),
        // invalid: '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first_  name")),
        // invalid: ' _' not allowed
        () -> assertThrows(MolgenisException.class, () -> new Column("first   _name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first  _  name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first  __   name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("aa    ____      ")));
  }
}
