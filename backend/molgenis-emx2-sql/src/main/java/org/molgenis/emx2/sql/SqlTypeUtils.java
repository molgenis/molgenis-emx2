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

  static Map<String, Object> getValuesAsMap(Row row, Collection<Column> columns) {
    try {
      Map<String, Object> values = new LinkedHashMap<>();
      for (Column c : columns) {

        Object value;

        // refConstraint == computed field
        if (c.getComputed() != null) {
          if (row.getValueMap().containsKey(c.getComputed())) {
            value = row.getValueMap().get(c.getComputed());
          } else {
            value = executeJavascriptOnRow(c.getComputed(), row);
          }
        } else {
          value = getTypedValue(row, c);
        }

        // get value
        if (Constants.MG_EDIT_ROLE.equals(c.getName())) {
          values.put(c.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
        } else {
          values.put(c.getName(), value);
        }
      }
      return values;
    } catch (MolgenisException me) {
      throw new MolgenisException("Parsing of row failed: " + row.toString(), me);
    }
  }

  public static Object getTypedValue(Row row, Column c) {
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
}
