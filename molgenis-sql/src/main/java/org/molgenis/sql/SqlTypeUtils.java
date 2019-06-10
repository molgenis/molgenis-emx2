package org.molgenis.sql;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.molgenis.Column;

import static org.jooq.impl.SQLDataType.*;

public class SqlTypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType typeOf(Column.Type sqlType) {
    switch (sqlType) {
      case UUID:
        return UUID;
      case STRING:
      case HYPERLINK:
      case EMAIL:
      case HTML:
      case FILE:
      case ENUM:
        return VARCHAR(255);
      case INT:
      case LONG:
        return INTEGER;
      case BOOL:
        return BOOLEAN;
      case DECIMAL:
        return DOUBLE;
      case TEXT:
        return LONGVARCHAR;
      case DATE:
        return DATE;
      case DATETIME:
        return TIMESTAMPWITHTIMEZONE;
      case REF:
      case SELECT:
      case RADIO:
        return UUID;
      case MREF:
      case CHECKBOX:
      case MSELECT:
      default:
        // should never happen
        throw new IllegalArgumentException("addColumn(name,type) : unsupported type " + sqlType);
    }
  }

  public static Column.Type getSqlType(Field f) {
    DataType type = f.getDataType().getSQLDataType();
    if (SQLDataType.UUID.equals(type)) return Column.Type.UUID;
    if (SQLDataType.VARCHAR.equals(type)) return Column.Type.STRING;
    if (SQLDataType.BOOLEAN.equals(type)) return Column.Type.BOOL;
    if (SQLDataType.INTEGER.equals(type)) return Column.Type.INT;
    if (SQLDataType.DOUBLE.equals(type)) return Column.Type.DECIMAL;
    if (SQLDataType.FLOAT.equals(type)) return Column.Type.DECIMAL;
    if (SQLDataType.LONGVARCHAR.equals(type)) return Column.Type.TEXT;
    if (SQLDataType.DATE.equals(type)) return Column.Type.DATE;
    if (SQLDataType.TIMESTAMPWITHTIMEZONE.equals(type)) return Column.Type.DATETIME;
    throw new UnsupportedOperationException(
        "Unsupported SQL type found:" + f.getDataType().getSQLType() + " " + f.getDataType());
  }
}
