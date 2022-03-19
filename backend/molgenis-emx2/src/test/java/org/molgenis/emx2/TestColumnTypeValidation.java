package org.molgenis.emx2;

import static org.junit.Assert.fail;
import static org.molgenis.emx2.ColumnType.EMAIL;
import static org.molgenis.emx2.ColumnType.HYPERLINK;

import org.junit.Test;

public class TestColumnTypeValidation {
  @Test
  public void testEmailValidation() {
    try {
      EMAIL.validate("test");
      fail("should fail");
    } catch (Exception e) {
      // correct
    }

    EMAIL.validate("test@home.nl");
  }

  @Test
  public void testHyperlinkValidation() {
    try {
      HYPERLINK.validate("test");
      fail("should fail");
    } catch (Exception e) {
      // correct
    }

    HYPERLINK.validate("http://test.com");
  }
}
