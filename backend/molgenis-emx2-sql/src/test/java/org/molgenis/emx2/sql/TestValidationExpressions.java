package org.molgenis.emx2.sql;

import static org.junit.Assert.*;

import org.junit.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;

public class TestValidationExpressions {

  @Test
  public void testValidationExpressions() {
    String exp = "{birthYear} <= {currentYear}";
    String exp2 = "{birthYear} + {currentYear}";

    Row row1 = new Row("birthYear", 1976, "currentYear", 2021);
    Row row2 = new Row("birthYear", 2022, "currentYear", 2021);

    assertTrue(EvaluateExpressions.check(exp, row1));
    assertFalse(EvaluateExpressions.check(exp, row2));
    assertEquals(1976 + 2021, (int) TypeUtils.toInt(EvaluateExpressions.compute(exp2, row1)));

    Column c = new Column("currentYear").setValidIf("{birthYear} <= {currentYear}");
  }
}
