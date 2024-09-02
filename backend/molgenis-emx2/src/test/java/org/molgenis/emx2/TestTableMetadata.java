package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestTableMetadata {
  @Test
  public void validTableMetadataName() {
    assertAll(
        // valid: 1 or more legal characters
        () -> assertDoesNotThrow(() -> new TableMetadata("a")),
        // valid: a space
        () -> assertDoesNotThrow(() -> new TableMetadata("first name")),
        // valid: space & underscore but not next to each other
        () -> assertDoesNotThrow(() -> new TableMetadata("yet_another name")),
        // invalid: # should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("#first name")),
        // invalid: '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first_  name")),
        // invalid: ' _' not allowed
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first   _name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first  _  name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first  __   name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("aa    ____      ")),
        // invalid: too long (> 31 characters)
        () ->
            assertThrows(
                MolgenisException.class,
                () -> new TableMetadata("abcdefghijklmnopqrstuvwzyx789012")));
  }
}
