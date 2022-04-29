package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.EvaluateExpressions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

public class EvaluateExpressionsTest extends TestCase {

  @Test
  public void testCheckValidationColumnsSuccess() {
    Collection<Column> columns = new ArrayList<>();
    Column column1 = new Column("columnName1");
    column1.setValidation("{columnName2}");
    Column column2 = new Column("columnName2");

    columns.add(column1);
    columns.add(column2);

    checkForMissingVariablesColumns(columns);
  }

  @Test
  public void testCheckValidationColumnsFailure() {
    Collection<Column> columns = new ArrayList<>();
    Column column1 = new Column("columnName1");
    column1.setValidation("{columnName2}");
    columns.add(column1);
    try {
      checkForMissingVariablesColumns(columns);
    } catch (MolgenisException exception) {
      String expectedError = "Validation failed: columns [columnName2] not provided";
      assertEquals(expectedError, exception.getMessage());
    }
  }

  @Test
  public void evaluateValidationExpressionTestSuccessLogical() {
    String expression = "false && true";
    Row row = new Row();
    assertTrue(evaluateValidationExpression(expression, row));
  }

  @Test
  public void evaluateValidationExpressionTestSuccessNumerical() {
    String expression = "5 + 37";
    Row row = new Row();
    assertTrue(evaluateValidationExpression(expression, row));
  }

  @Test
  public void evaluateValidationExpressionTestSuccessFunctionCall() {
    String expression = "today()";
    Row row = new Row();
    assertTrue(evaluateValidationExpression(expression, row));
  }

  @Test
  public void evaluateValidationExpressionTestFailure() {
    String expression = "invalid input";
    Row row = new Row();
    assertFalse(evaluateValidationExpression(expression, row));
  }

  @Test
  public void testCalculateComputedExpression() {
    String expression = "5 + 7";
    Row row = new Row();
    Object outcome = calculateComputedExpression(expression, row);
    assertEquals(12.0, outcome);
  }

  @Test
  public void testCalculateComputedExpressionFailure() {
    String expression = "5 + YAAARGH";
    Row row = new Row();
    try {
      calculateComputedExpression(expression, row);
    } catch (MolgenisException exception) {
      assertEquals("Failed to execute expression: " + expression, exception.getMessage());
    }
  }

  @Test
  public void testCheckValidationSuccess() {
    Map<String, Object> values = new HashMap<>();
    Collection<Column> columns = new ArrayList<>();
    String validation = "true && true";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    checkValidation(values, columns);
  }

  @Test
  public void testCheckValidationInvalidExpression() {
    Map<String, Object> values = new HashMap<>();
    Collection<Column> columns = new ArrayList<>();
    String validation = "this is very invalid";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    try {
      checkValidation(values, columns);
    } catch (MolgenisException exception) {
      assertEquals("Cannot execute expression: this is very invalid", exception.getMessage());
    }
  }

  @Test
  public void testCheckValidationTurnToBoolIsFalse() {
    Map<String, Object> values = new HashMap<>();
    Collection<Column> columns = new ArrayList<>();
    String validation = "false";
    Column column = new Column("name");
    column.setValidation(validation);
    columns.add(column);
    try {
      checkValidation(values, columns);
    } catch (MolgenisException exception) {
      assertEquals("false. Values provided: {}", exception.getMessage());
    }
  }
}
