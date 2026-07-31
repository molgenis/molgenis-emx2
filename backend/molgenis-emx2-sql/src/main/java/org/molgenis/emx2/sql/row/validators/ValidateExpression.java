package org.molgenis.emx2.sql.row.validators;

import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.molgenis.emx2.utils.TypeUtils;

public class ValidateExpression {

  private ValidateExpression() {
    // hide constructor
  }

  public static void apply(Map<String, Object> context, Column column) {
    if (context.get(column.getIdentifier()) != null) {
      column.getColumnType().validate(context.get(column.getIdentifier()));
      TypeUtils.checkEnumMembership(column, context.get(column.getIdentifier()));
      if (column.getValidation() != null) {
        String errorMessage = SqlTypeUtils.checkValidation(column.getValidation(), context);
        if (errorMessage != null)
          throw new MolgenisException(
              "Validation error on column '" + column.getName() + "': " + errorMessage + ".");
      }
    }
  }
}
