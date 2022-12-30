package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.EvaluateExpressions.checkValidation;
import static org.molgenis.emx2.sql.SqlTypeUtils.getValuesAsMap;
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

    String result = checkValidation("columnName2 > 1", Map.of("columnName2", 1));
    assertEquals("Validation failed: columnName2 > 1", result);

    result = checkValidation("/^([a-z]+)$/.test(name)", Map.of("name", "123"));
    assertEquals("Validation failed: /^([a-z]+)$/.test(name)", result);

    result = checkValidation("/^([a-z]+)$/.test(name)", Map.of("name", "abc"));
    assertNull(result);

    result =
        checkValidation(
            "if(!/^([a-z]+)$/.test(name))'name should contain only lowercase letters'",
            Map.of("name", "123"));
    assertEquals("Validation failed: name should contain only lowercase letters", result);

    result =
        checkValidation(
            "if(!/^([a-z]+)$/.test(name))'name should contain only lowercase letters'",
            Map.of("name", "abc"));
    assertNull(result);
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
    String outcome = checkValidation(expression, Map.of());
    assertEquals(12, Integer.parseInt(outcome));
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
    getValuesAsMap(new Row(), columns);
  }

  @Test
  public void testCheckValidationInvalidExpression() {
    Collection<Column> columns = new ArrayList<>();
    String validation = "this is very invalid";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    try {
      getValuesAsMap(new Row(), columns);
    } catch (MolgenisException exception) {
      assertEquals("Cannot execute expression: this is very invalid", exception.getMessage());
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
      getValuesAsMap(new Row(), columns);
    } catch (MolgenisException exception) {
      assertEquals("false. Values provided: {}", exception.getMessage());
    }
  }
}
