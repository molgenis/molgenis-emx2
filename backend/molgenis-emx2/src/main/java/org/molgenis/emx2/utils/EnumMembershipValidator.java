package org.molgenis.emx2.utils;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;

public class EnumMembershipValidator {

  private EnumMembershipValidator() {}

  public static void checkEnumMembership(Column column, Object value) {
    List<String> allowedValues = column.getAllowedValues();
    if (value instanceof Object[] elements) {
      for (Object element : elements) {
        checkAllowed(String.valueOf(element), allowedValues, column);
      }
    } else {
      checkAllowed(value.toString(), allowedValues, column);
    }
  }

  private static void checkAllowed(String value, List<String> allowedValues, Column column) {
    if (!allowedValues.contains(value)) {
      throw new MolgenisException(
          "Value '"
              + value
              + "' is not allowed for column '"
              + column.getName()
              + "'. Allowed values: "
              + allowedValues);
    }
  }
}
