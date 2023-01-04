package org.molgenis.emx2.sql;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnRow;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class SqlTypeUtils extends TypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  static Map<String, Object> validateAndGetVisibleValuesAsMap(Row row, Collection<Column> columns) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (Column c : columns) {

      // we get role from environment
      if (Constants.MG_EDIT_ROLE.equals(c.getName())) {
        values.put(c.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
      } else
      // compute field, might depend on update values therefor run always on insert/update
      if (c.getComputed() != null) {
        values.put(c.getName(), executeJavascriptOnRow(c.getComputed(), row));
      } else
      // otherwise, unless invisible
      if (columnIsVisible(c, row)) {
        checkValidation(c, row);
        values.put(c.getName(), getTypedValue(c, row));
      }
    }
    return values;
  }

  private static boolean columnIsVisible(Column column, Row row) {
    if (column.getVisible() != null) {
      Object visibleResult = executeJavascriptOnRow(column.getVisible(), row);
      if (Boolean.FALSE.equals(visibleResult) || visibleResult == null) {
        return false;
      }
    }
    return true;
  }

  public static Object getTypedValue(Column c, Row row) {
    String name = c.getName();
    return switch (c.getPrimitiveColumnType()) {
      case FILE -> row.getBinary(name);
      case UUID -> row.getUuid(name);
      case UUID_ARRAY -> row.getUuidArray(name);
      case STRING, EMAIL, HYPERLINK -> row.getString(name);
      case STRING_ARRAY, EMAIL_ARRAY, HYPERLINK_ARRAY -> row.getStringArray(name);
      case BOOL -> row.getBoolean(name);
      case BOOL_ARRAY -> row.getBooleanArray(name);
      case INT -> row.getInteger(name);
      case INT_ARRAY -> row.getIntegerArray(name);
      case LONG -> row.getLong(name);
      case LONG_ARRAY -> row.getLongArray(name);
      case DECIMAL -> row.getDecimal(name);
      case DECIMAL_ARRAY -> row.getDecimalArray(name);
      case TEXT -> row.getText(name);
      case TEXT_ARRAY -> row.getTextArray(name);
      case DATE -> row.getDate(name);
      case DATE_ARRAY -> row.getDateArray(name);
      case DATETIME -> row.getDateTime(name);
      case DATETIME_ARRAY -> row.getDateTimeArray(name);
      case JSONB -> row.getJsonb(name);
      case JSONB_ARRAY -> row.getJsonbArray(name);
      default -> throw new UnsupportedOperationException(
          "Unsupported columnType found:" + c.getColumnType());
    };
  }

  static String getPsqlType(Column column) {
    return getPsqlType(column.getPrimitiveColumnType());
  }

  static String getPsqlType(ColumnType type) {
    return switch (type) {
      case STRING, EMAIL, HYPERLINK, TEXT -> "character varying";
      case STRING_ARRAY, EMAIL_ARRAY, HYPERLINK_ARRAY, TEXT_ARRAY -> "character varying[]";
      case UUID -> "uuid";
      case UUID_ARRAY -> "uuid[]";
      case BOOL -> "bool";
      case BOOL_ARRAY -> "bool[]";
      case INT -> "int";
      case INT_ARRAY -> "int[]";
      case LONG -> "bigint";
      case LONG_ARRAY -> "bigint[]";
      case DECIMAL -> "decimal";
      case DECIMAL_ARRAY -> "decimal[]";
      case DATE -> "date";
      case DATE_ARRAY -> "date[]";
      case DATETIME -> "timestamp without time zone";
      case DATETIME_ARRAY -> "timestamp without time zone[]";
      case JSONB -> "jsonb";
      default -> throw new MolgenisException(
          "Unknown type: Internal error: data cannot be mapped to psqlType " + type);
    };
  }

  public static void checkValidation(Column column, Row row) {
    if (!row.isNull(column.getName(), column.getColumnType())) {
      Map<String, Object> values = row.getValueMap();
      column.getColumnType().validate(row.get(column));

      // validation
      if (column.getValidation() != null) {
        String errorMessage = checkValidation(column.getValidation(), values);
        if (errorMessage != null)
          throw new MolgenisException(
                  "Validation error on column '" + column.getName() + "': " + errorMessage + ".");
      }
    }
  }

  public static String checkValidation(String validationScript, Map<String, Object> values) {
    try {
      Object error = executeJavascriptOnRow(validationScript, new Row(values));
      if (error != null) {
        if (Boolean.FALSE.equals(error)) {
          // you can have a validation rule that simply returns true or false; false means not
          // valid.
          return validationScript;
        } else
          // you can have a validation script returning true which means valid, so false means error.
          if (!(error instanceof Boolean)) {
            return error.toString();
          }
      }
      return null;
    } catch (MolgenisException me) {
      // seperate syntax errors
      throw me;
    }
  }
}
