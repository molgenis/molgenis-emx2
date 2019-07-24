package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.molgenis.Column;

import static org.jooq.impl.SQLDataType.*;

public class SqlTypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType typeOf(DSLContext jooq, Column.Type sqlType) {

    switch (sqlType) {
      case UUID:
        return UUID.getDataType(jooq.configuration());
      case STRING:
        //      case HYPERLINK:
        //      case EMAIL:
        //      case HTML:
        //      case FILE:
        //      case ENUM:
        return VARCHAR(255).getDataType(jooq.configuration());
      case INT:
        //      case LONG:
        return INTEGER.getDataType(jooq.configuration());

      case BOOL:
        return BOOLEAN.getDataType(jooq.configuration());

      case DECIMAL:
        return DOUBLE.getDataType(jooq.configuration());

      case TEXT:
        return LONGVARCHAR.getDataType(jooq.configuration());

      case DATE:
        return DATE.getDataType(jooq.configuration());

      case DATETIME:
        return TIMESTAMPWITHTIMEZONE.getDataType(jooq.configuration());

      case ENUM:
        return VARCHAR(255).getDataType(jooq.configuration());
        // TODO discuss if we want to use proper PG types for this
        // https://www.postgresql.org/docs/9.1/datatype-enum.html
      case REF:
        //      case SELECT:
        //      case RADIO:
        return UUID.getDataType(jooq.configuration());

      case MREF:
        //      case CHECKBOX:
        //      case MSELECT:
        return UUID.getDataType(jooq.configuration());

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
