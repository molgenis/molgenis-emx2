package org.molgenis.emx2.sql;

import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnRow;

public class EvaluateExpressions {

  private EvaluateExpressions() {
    // hide constructor
  }

  public static void checkValidation(Map<String, Object> values, Collection<Column> columns) {
    for (Column column : columns) {
      // system based validation
      column.getColumnType().validate(values.get(column.getName()));

      // validation
      if (column.getValidation() != null) {
        String errorMessage = checkValidation(column.getValidation(), values);
        if (errorMessage != null) {
          if (errorMessage != null)
            throw new MolgenisException(
                    "Validation error on column '" + column.getValidation() + "'" + errorMessage + ".");
        }
      }
    }
  }

  public static String checkValidation(String validationScript, Map<String, Object> values) {
    Object error = executeJavascriptOnRow(validationScript, new Row(values));
    if (error != null) {
      if (Boolean.FALSE.equals(error)) {
        // you can have a validation rule that simply returns true or false; false means not
        // valid.
        return "Validation failed: " + validationScript;
      } else
        // you can have a validiation script returning true which means valid
        if (!Boolean.TRUE.equals(error)) {
          return error.toString();
        }
    }
    return null;
  }
}
