package org.molgenis.emx2.sql.row.validators;

import java.util.Arrays;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;

public class ValidateAllowedValues {

  private ValidateAllowedValues() {
    // hide constructor
  }

  public static void apply(Map<String, Object> context, Column column) {
    Object value = context.get(column.getIdentifier());
    if (!column.getColumnType().isEnum() || value == null) {
      return;
    }
    String[] allowed = column.getAllowedValues();
    Object[] elements = value instanceof Object[] array ? array : new Object[] {value};
    for (Object element : elements) {
      if (!Arrays.asList(allowed).contains(String.valueOf(element))) {
        throw new MolgenisException(
            "Value '"
                + element
                + "' is not allowed for column '"
                + column.getName()
                + "'. Allowed values: '"
                + String.join("', '", allowed)
                + "'");
      }
    }
  }
}
