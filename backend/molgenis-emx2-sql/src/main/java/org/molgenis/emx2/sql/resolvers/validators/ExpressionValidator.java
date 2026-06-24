package org.molgenis.emx2.sql.resolvers.validators;

import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.SqlTypeUtils;

public class ExpressionValidator implements RowValidator {

  private final Map<String, Object> context;

  public ExpressionValidator(Map<String, Object> context) {
    this.context = context;
  }

  @Override
  public void apply(Column column, Row row) {
    checkValidation(column);
  }

  public void checkValidation(Column column) {
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
