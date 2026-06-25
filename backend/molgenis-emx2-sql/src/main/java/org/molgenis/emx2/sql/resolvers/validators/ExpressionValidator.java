package org.molgenis.emx2.sql.resolvers.validators;

import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.SqlTypeUtils;

public class ExpressionValidator {

  private ExpressionValidator() {
    throw new AssertionError("Can't instantiate utility class");
  }

  public static void apply(Map<String, Object> context, Column column, Row row) {
    checkValidation(context, column);
  }

  public static void checkValidation(Map<String, Object> context, Column column) {
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
