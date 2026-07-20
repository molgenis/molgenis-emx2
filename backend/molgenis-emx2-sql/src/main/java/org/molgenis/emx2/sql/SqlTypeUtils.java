package org.molgenis.emx2.sql;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class SqlTypeUtils extends TypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static Object getTypedValue(Column c, Row row) {
    String name = c.getName();
    return switch (c.getPrimitiveColumnType()) {
      case FILE -> row.getBinary(name);
      case UUID -> row.getUuid(name);
      case UUID_ARRAY -> row.getUuidArray(name);
      case STRING, ENUM -> row.getString(name);
      case STRING_ARRAY, ENUM_ARRAY -> row.getStringArray(name);
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
      case PERIOD -> row.getPeriod(name);
      case PERIOD_ARRAY -> row.getPeriodArray(name);
      case JSON -> row.getJsonb(name);
      default ->
          throw new UnsupportedOperationException(
              "Unsupported columnType found:" + c.getColumnType());
    };
  }

  static String getPsqlType(Column column) {
    return getPsqlType(column.getPrimitiveColumnType());
  }

  static String getPsqlType(ColumnType type) {
    return switch (type.getBaseType()) {
      case STRING, TEXT, ENUM -> "character varying";
      case STRING_ARRAY, TEXT_ARRAY, ENUM_ARRAY -> "character varying[]";
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
      case JSON -> "jsonb";
      default ->
          throw new MolgenisException(
              "Unknown type: Internal error: data cannot be mapped to psqlType " + type);
    };
  }

  public static String checkValidation(String validationScript, Map<String, Object> values) {
    try {
      Object error = executeJavascriptOnMap(validationScript, values);
      if (error != null) {
        if (error instanceof Boolean && (Boolean) error == false) {
          // you can have a validation rule that simply returns true or false;
          // false means not valid.
          return validationScript;
        } else if (error instanceof Boolean && (Boolean) error == true) {
          return null;
        }
        // you can have a validation script returning true which means valid, and undefined also
        else {
          return error.toString();
        }
      } else {
        return null;
      }
    } catch (MolgenisException me) {
      // seperate syntax errors
      throw me;
    }
  }
}
