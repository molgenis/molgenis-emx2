package org.molgenis.sql;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.SQLDataType.*;

class SqlTypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType typeOf(SqlType sqlType) {
    switch (sqlType) {
      case UUID:
        return UUID;
      case STRING:
        return VARCHAR(255);
      case INT:
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
        return UUID;
      default:
        // should never happen
        throw new IllegalArgumentException("addColumn(name,type) : unsupported type " + sqlType);
    }
  }

  public static SqlType getSqlType(Field f) {
    DataType type = f.getDataType().getSQLDataType();
    if (SQLDataType.UUID.equals(type)) return SqlType.UUID;
    if (SQLDataType.VARCHAR.equals(type)) return SqlType.STRING;
    if (SQLDataType.BOOLEAN.equals(type)) return SqlType.BOOL;
    if (SQLDataType.INTEGER.equals(type)) return SqlType.INT;
    if (SQLDataType.DOUBLE.equals(type)) return SqlType.DECIMAL;
    if (SQLDataType.LONGVARCHAR.equals(type)) return SqlType.TEXT;
    if (SQLDataType.DATE.equals(type)) return SqlType.DATE;
    if (SQLDataType.TIMESTAMPWITHTIMEZONE.equals(type)) return SqlType.DATETIME;
    throw new UnsupportedOperationException(
        "Unsupported SQL type found:" + f.getDataType().getSQLType());
  }
}
