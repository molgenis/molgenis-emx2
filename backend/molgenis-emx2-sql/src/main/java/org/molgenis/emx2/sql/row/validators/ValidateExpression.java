package org.molgenis.emx2.sql.row.validators;

import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlTypeUtils;

public class ValidateExpression {

  private ValidateExpression() {
    throw new AssertionError("Can't instantiate utility class");
  }

  public static void apply(Map<String, Object> context, Column column) {
    if (context.get(column.getIdentifier()) != null) {
      column.getColumnType().validate(context.get(column.getName()));
      if (column.getValidation() != null) {
        String errorMessage = SqlTypeUtils.checkValidation(column.getValidation(), context);
        if (errorMessage != null)
          throw new MolgenisException(
              "Validation error on column '" + column.getName() + "': " + errorMessage + ".");
      }
    }
  }
}
