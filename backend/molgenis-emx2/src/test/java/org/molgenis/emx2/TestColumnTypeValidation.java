package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.ColumnType.EMAIL;
import static org.molgenis.emx2.ColumnType.HYPERLINK;

import org.junit.jupiter.api.Test;

public class TestColumnTypeValidation {
  @Test
  public void testEmailValidation() {
    try {
      EMAIL.validate("test");
    } catch (MolgenisException e) {
      assertEquals("Validation failed: test is not valid EMAIL", e.getMessage());
    }

    EMAIL.validate("c.stroomberg@rug.com");
    EMAIL.validate("teSt@hoMe.nL");
    EMAIL.validate("test+test@home.nl");
    EMAIL.validate("üse@bar.com");
  }

  @Test
  public void testHyperlinkValidation() {
    try {
      HYPERLINK.validate("test");
    } catch (MolgenisException e) {
      assertEquals("Validation failed: test is not valid HYPERLINK", e.getMessage());
    }

    HYPERLINK.validate("http://test.com");
  }
}
