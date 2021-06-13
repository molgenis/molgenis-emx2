package org.molgenis.emx2.sql;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.expression.Expressions;
import scala.util.Try;

public class EvaluateExpressions {

  private static Expressions evaluator = new Expressions(10000);

  public static void checkValidationColumns(Set<Column> columns) {
    // get all expressions
    List<String> expressionList =
        columns.stream()
            .filter(c -> c.getValidIf() != null)
            .map(c -> c.getValidIf())
            .collect(Collectors.toList());

    // get all variables
    Set<String> variableNames = evaluator.getAllVariableNames(expressionList);

    // get all column names
    Set<String> columnNames = columns.stream().map(c -> c.getName()).collect(Collectors.toSet());

    // check if any variable is missing in column list
    Set<String> missing =
        variableNames.stream().filter(v -> !columnNames.contains(v)).collect(Collectors.toSet());
    if (missing.size() > 0) {
      throw new MolgenisException("Validation failed: columns " + missing + " not provided");
    }
  }

  public static boolean check(String expression, Row row) {
    Try<Object> result = evaluator.parseAndEvaluate(List.of(expression), row.getValueMap()).get(0);
    if (result.isFailure()) {
      throw new MolgenisException("Failed to execute expression: " + expression);
    }
    return TypeUtils.toBool(result.get());
  }

  public static Object compute(String expression, Row row) {
    Try<Object> result = evaluator.parseAndEvaluate(List.of(expression), row.getValueMap()).get(0);
    if (result.isFailure()) {
      throw new MolgenisException("Failed to execute expression: " + expression);
    }
    return result.get();
  }
}
