package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class TableMetadataTest {

  @Test
  void rlsFlagDefault() {
    assertFalse(new TableMetadata("MyTable").getRowLevelSecurity());
  }
}
