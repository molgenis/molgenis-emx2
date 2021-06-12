package org.molgenis.emx2.sql;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnRow;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnValue;

import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
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

        // validation
        if (value != null && c.getValidation() != null) {
          String error = executeJavascriptOnValue(c.getValidation(), value);
          if (error != null)
            throw new MolgenisException(
                "Validation error on column '"
                    + c.getName()
                    + "'"
                    + error
                    + ". Instead found value '"
                    + value
                    + "'");
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
    switch (c.getPrimitiveColumnType()) {
      case FILE:
        return row.getBinary(name);
      case UUID:
        return row.getUuid(name);
      case UUID_ARRAY:
        return row.getUuidArray(name);
      case STRING:
        return row.getString(name);
      case STRING_ARRAY:
        return row.getStringArray(name);
      case BOOL:
        return row.getBoolean(name);
      case BOOL_ARRAY:
        return row.getBooleanArray(name);
      case INT:
        return row.getInteger(name);
      case INT_ARRAY:
        return row.getIntegerArray(name);
      case DECIMAL:
        return row.getDecimal(name);
      case DECIMAL_ARRAY:
        return row.getDecimalArray(name);
      case TEXT:
        return row.getText(name);
      case TEXT_ARRAY:
        return row.getTextArray(name);
      case DATE:
        return row.getDate(name);
      case DATE_ARRAY:
        return row.getDateArray(name);
      case DATETIME:
        return row.getDateTime(name);
      case DATETIME_ARRAY:
        return row.getDateTimeArray(name);
      case JSONB:
        return row.getJsonb(name);
      case JSONB_ARRAY:
        return row.getJsonbArray(name);
      default:
        throw new UnsupportedOperationException(
            "Unsupported columnType found:" + c.getColumnType());
    }
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
