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

  static Collection<Object> getValuesAsCollection(Row row, Collection<Column> columns) {
    return getValuesAsMap(row, columns).values();
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
      case STRING -> row.getString(name);
      case STRING_ARRAY, EMAIL_ARRAY -> row.getStringArray(name);
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
    switch (type) {
      case STRING:
        return "character varying";
      case STRING_ARRAY:
        return "character varying[]";
      case UUID:
        return "uuid";
      case UUID_ARRAY:
        return "uuid[]";
      case BOOL:
        return "bool";
      case BOOL_ARRAY:
        return "bool[]";
      case INT:
        return "int";
      case INT_ARRAY:
        return "int[]";
      case LONG:
        return "bigint";
      case LONG_ARRAY:
        return "bigint[]";
      case DECIMAL:
        return "decimal";
      case DECIMAL_ARRAY:
        return "decimal[]";
      case TEXT:
        return "character varying";
      case TEXT_ARRAY:
        return "character varying[]";
      case DATE:
        return "date";
      case DATE_ARRAY:
        return "date[]";
      case DATETIME:
        return "timestamp without time zone";
      case DATETIME_ARRAY:
        return "timestamp without time zone[]";
      case JSONB:
        return "jsonb";
      default:
        throw new MolgenisException(
            "Unknown type: Internal error: data cannot be mapped to psqlType " + type);
    }
  }
}
