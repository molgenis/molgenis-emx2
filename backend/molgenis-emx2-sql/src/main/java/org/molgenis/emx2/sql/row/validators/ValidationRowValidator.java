package org.molgenis.emx2.sql.row.validators;

import static org.molgenis.emx2.ColumnType.AUTO_ID;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.molgenis.emx2.sql.row.computers.RowToMapConverter;

public class ValidationRowValidator implements RowValidator {

  private static final RowToMapConverter converter = new RowToMapConverter();

  @Override
  public void apply(List<Column> columns, Row row) {
    Map<String, Object> graph = converter.convertRowToMap(columns, row);

    List<Column> toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .toList();

    for (Column c : toValidateAndCompute) {
      checkValidation(c, graph);
    }
  }

  public static void checkValidation(Column column, Map<String, Object> values) {
    if (values.get(column.getIdentifier()) != null) {
      column.getColumnType().validate(values.get(column.getName()));
      // validation
      if (column.getValidation() != null) {
        // check if validation script contains js functions that are bound to java functions
        String errorMessage = SqlTypeUtils.checkValidation(column.getValidation(), values);
        if (errorMessage != null)
          throw new MolgenisException(
              "Validation error on column '" + column.getName() + "': " + errorMessage + ".");
      }
    }
  }
}
