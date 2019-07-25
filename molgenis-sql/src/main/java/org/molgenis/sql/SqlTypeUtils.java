package org.molgenis.sql;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.Table;

import java.util.ArrayList;
import java.util.Collection;

import static org.jooq.impl.SQLDataType.*;
import static org.molgenis.Column.Type.MREF;
import static org.molgenis.Column.Type.REF;

public class SqlTypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType typeOf(Column column) throws MolgenisException {

    Column.Type sqlType = column.getType();
    switch (sqlType) {
      case UUID:
        return UUID;
      case STRING:
        //      case HYPERLINK:
        //      case EMAIL:
        //      case HTML:
        //      case FILE:
        //      case ENUM:
        return VARCHAR(255);
      case INT:
        //      case LONG:
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
        return TIMESTAMP;
      case ENUM:
        return VARCHAR(255);
        // TODO discuss if we want to use proper PG types for this
        // https://www.postgresql.org/docs/9.1/datatype-enum.html
      case REF:
        //      case SELECT:
        //      case RADIO:
        return typeOf(
            column
                .getTable()
                .getSchema()
                .getTable(column.getRefTable())
                .getColumn(column.getRefColumn()));

      case MREF:
        //      case CHECKBOX:
        //      case MSELECT:
        return UUID;

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

  public static Collection<Object> getValuesAsCollection(Row row, Table table)
      throws MolgenisException {
    Collection<Object> values = new ArrayList<>();
    for (Column c : table.getColumns()) {
      // TODO: fix MREF
      if (!MREF.equals(c.getType())) values.add(getTypedValue(row, c));
    }
    return values;
  }

  public static Object getTypedValue(Row row, Column column) throws MolgenisException {
    Column.Type type = column.getType();
    if (REF.equals(type)) {
      type = column.getRefType();
    }
    switch (type) {
      case STRING:
        return row.getString(column.getName());
      case UUID:
        return row.getUuid(column.getName());
      case BOOL:
        return row.getBool(column.getName());
      case INT:
        return row.getInt(column.getName());
      case DECIMAL:
        return row.getDecimal(column.getName());
      case TEXT:
        return row.getText(column.getName());
      case DATE:
        return row.getDate(column.getName());
      case DATETIME:
        return row.getDateTime(column.getName());
      default:
        throw new UnsupportedOperationException("Unsupported type found:" + column.getType());
    }
  }
}
