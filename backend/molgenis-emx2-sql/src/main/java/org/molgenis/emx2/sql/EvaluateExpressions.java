package org.molgenis.emx2.sql;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.expression.Expressions;
import scala.util.Try;

import java.util.*;
import java.util.stream.Collectors;

public class EvaluateExpressions {

  private static final Expressions evaluator = new Expressions(10000);

  /**
   * validate if the expression is valid, given the metadata. Typically, done at beginning of a
   * batch transaction
   */
  public static void checkValidationColumns(Collection<Column> columns) {
    List<String> expressionList = getExpressionList(columns);
    Set<String> variableNames = evaluator.getAllVariableNames(expressionList);
    Set<String> columnNames = getColumnNames(columns);

    // check if any variable is missing in column list
    Set<String> missingVariables =
        variableNames.stream().filter(v -> !columnNames.contains(v)).collect(Collectors.toSet());
    if (!missingVariables.isEmpty()) {
      throw new MolgenisException(
          "Validation failed: columns " + missingVariables + " not provided");
    }
  }

  private static Set<String> getColumnNames(Collection<Column> columns) {
    return columns.stream().map(Column::getName).collect(Collectors.toSet());
  }

  private static List<String> getExpressionList(Collection<Column> columns) {
    return columns.stream().map(Column::getValidation).filter(Objects::nonNull).toList();
  }

  /** validate an expression given a row. True means it is valid. */
  public static boolean check(String expression, Row row) {
    return TypeUtils.toBool(compute(expression, row));
  }

  /** use expression to compute value and return value of the expression */
  public static Object compute(String expression, Row row) {
    Try<Object> result = evaluator.parseAndEvaluate(List.of(expression), row.getValueMap()).get(0);
    if (result.isFailure()) {
      throw new MolgenisException("Failed to execute expression: " + expression);
    }
    return result.get();
  }

  public static void checkValidation(Map<String, Object> values, Collection<Column> columns) {
    for (Column c : columns) {
      if (c.getValidation() != null) {
        Try<Object> result = evaluator.parseAndEvaluate(List.of(c.getValidation()), values).get(0);
        if (result.isFailure()) {
          throw new MolgenisException(
              String.format("Cannot execute expression: %s", c.getValidation()));
        }
        if (!TypeUtils.toBool(result.get())) {
          throw new MolgenisException(
              String.format("%s. Values provided: %s", c.getValidation(), values));
        }
      }
    }
  }
}
