package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.SqlTypeUtils.checkValidation;
import static org.molgenis.emx2.sql.SqlTypeUtils.validateAndGetVisibleValuesAsMap;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

public class TestEvaluateExpressions extends TestCase {

  @Test
  public void testCheckValidationColumnsSuccess() {
    // should not throw exception
    executeJavascriptOnRow("columnName2", Row.row("columnName2", true));
    executeJavascriptOnRow("columnName2 > 1", Row.row("columnName2", 2));

    String error = checkValidation("columnName2 > 1", Map.of("columnName2", 1));
    assertEquals("columnName2 > 1", error);

    error = checkValidation("/^([a-z]+)$/.test(name)", Map.of("name", "123"));
    assertEquals("/^([a-z]+)$/.test(name)", error);

    error = checkValidation("/^([a-z]+)$/.test(name)", Map.of("name", "abc"));
    assertNull(error);

    error =
        checkValidation(
            "if(!/^([a-z]+)$/.test(name))'name should contain only lowercase letters'",
            Map.of("name", "123"));
    assertEquals("name should contain only lowercase letters", error);

    error =
        checkValidation(
            "if(!/^([a-z]+)$/.test(name))'name should contain only lowercase letters'",
            Map.of("name", "abc"));
    assertNull(error);
  }

  @Test
  public void testCheckValidationColumnsFailure() {
    try {
      checkValidation("columnName2", Map.of());
      fail("should throw exception");
    } catch (MolgenisException exception) {
      String expectedError = "script failed: ReferenceError: columnName2 is not defined";
      assertEquals(expectedError, exception.getMessage());
    }
  }

  @Test
  public void evaluateValidationExpressionTestSuccessLogical() {
    String expression = "false && true";
    Row row = new Row();
    assertNotNull(checkValidation(expression, Map.of()));
  }

  @Test
  public void evaluateValidationExpressionTestSuccessNumerical() {
    String expression = "5 + 37";
    Row row = new Row();
    assertNotNull(checkValidation(expression, Map.of()));
  }

  @Test
  public void evaluateValidationExpressionTestSuccessFunctionCall() {
    String expression = "today()";
    Row row = new Row();
    assertNotNull(checkValidation(expression, Map.of()));
  }

  @Test
  public void evaluateValidationExpressionTestFailure() {
    String expression = "invalid input";
    assertNull(checkValidation(expression, Map.of()));
  }

  @Test
  public void testCalculateComputedExpression() {
    String expression = "5 + 7";
    Object outcome = executeJavascriptOnRow(expression, new Row());
    assertEquals(12, outcome);
  }

  @Test
  public void testCalculateComputedExpressionFailure() {
    String expression = "5 + YAAARGH";
    try {
      checkValidation(expression, Map.of());
    } catch (MolgenisException exception) {
      assertTrue(exception.getMessage().contains("YAAARGH is not defined"));
    }
  }

  @Test
  public void testCheckValidationSuccess() {
    Collection<Column> columns = new ArrayList<>();
    String validation = "true && true";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    validateAndGetVisibleValuesAsMap(new Row(), columns);
  }

  @Test
  public void testCheckValidationInvalidExpression() {
    Collection<Column> columns = new ArrayList<>();
    String validation = "this is very invalid";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    try {
      validateAndGetVisibleValuesAsMap(new Row("name", "test"), columns);
    } catch (MolgenisException exception) {
      assertEquals(
          "script failed: SyntaxError: <eval>:1:5 Expected ; but found is\n"
              + "this is very invalid\n"
              + "     ^\n",
          exception.getMessage());
    }
  }

  @Test
  public void testCheckValidationTurnToBoolIsFalse() {
    Collection<Column> columns = new ArrayList<>();
    String validation = "false";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    try {
      validateAndGetVisibleValuesAsMap(new Row("name", "test"), columns);
    } catch (MolgenisException exception) {
      assertEquals("Validation error on column 'name': false.", exception.getMessage());
    }
  }
}
