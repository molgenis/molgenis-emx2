package org.molgenis.sql;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Support;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.molgenis.Type.UUID_ARRAY;

public class SqlTypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType jooqTypeOf(Column column) throws MolgenisException {
    Type sqlType = column.getType();
    switch (sqlType) {
      case UUID:
        return SQLDataType.UUID;
      case UUID_ARRAY:
        return SQLDataType.UUID.getArrayDataType();
      case STRING:
        return SQLDataType.VARCHAR(255);
      case STRING_ARRAY:
        return SQLDataType.VARCHAR(255).getArrayDataType();
      case INT:
        return SQLDataType.INTEGER;
      case INT_ARRAY:
        return SQLDataType.INTEGER.getArrayDataType();
      case BOOL:
        return SQLDataType.BOOLEAN;
      case BOOL_ARRAY:
        return SQLDataType.BOOLEAN.getArrayDataType();
      case DECIMAL:
        return SQLDataType.DOUBLE;
      case DECIMAL_ARRAY:
        return SQLDataType.DOUBLE.getArrayDataType();
      case TEXT:
        return SQLDataType.LONGVARCHAR;
      case TEXT_ARRAY:
        return SQLDataType.LONGVARCHAR.getArrayDataType();
      case DATE:
        return SQLDataType.DATE;
      case DATE_ARRAY:
        return SQLDataType.DATE.getArrayDataType();
      case DATETIME:
        return SQLDataType.TIMESTAMP;
      case DATETIME_ARRAY:
        return SQLDataType.TIMESTAMP.getArrayDataType();
      case REF:
        return jooqTypeOf(
            column
                .getTable()
                .getSchema()
                .getTable(column.getRefTable())
                .getColumn(column.getRefColumn()));
      case REF_ARRAY:
        return jooqTypeOf(
                column
                    .getTable()
                    .getSchema()
                    .getTable(column.getRefTable())
                    .getColumn(column.getRefColumn()))
            .getArrayDataType();
      case MREF:
        return jooqTypeOf(
                column
                    .getTable()
                    .getSchema()
                    .getTable(column.getRefTable())
                    .getColumn(column.getRefColumn()))
            .getArrayDataType();
      default:
        // should never happen
        throw new IllegalArgumentException("addColumn(name,type) : unsupported type " + sqlType);
    }
  }

  public static Type getSqlType(Field f) {
    DataType type = f.getDataType().getSQLDataType();

    if (SQLDataType.UUID.equals(type)) return Type.UUID;
    if (SQLDataType.VARCHAR.equals(type)) return Type.STRING;
    if (SQLDataType.BOOLEAN.equals(type)) return Type.BOOL;
    if (SQLDataType.INTEGER.equals(type)) return Type.INT;
    if (SQLDataType.DOUBLE.equals(type)) return Type.DECIMAL;
    if (SQLDataType.FLOAT.equals(type)) return Type.DECIMAL;
    if (SQLDataType.LONGVARCHAR.equals(type)) return Type.TEXT;
    if (SQLDataType.DATE.equals(type)) return Type.DATE;
    if (SQLDataType.TIMESTAMPWITHTIMEZONE.equals(type)) return Type.DATETIME;
    if (SQLDataType.UUID.getArrayDataType().equals(type)) return UUID_ARRAY;
    if (SQLDataType.VARCHAR.getArrayDataType().equals(type)) return Type.STRING;
    if (SQLDataType.BOOLEAN.getArrayDataType().equals(type)) return Type.BOOL;
    if (SQLDataType.INTEGER.getArrayDataType().equals(type)) return Type.INT;
    if (SQLDataType.DOUBLE.getArrayDataType().equals(type)) return Type.DECIMAL;
    if (SQLDataType.FLOAT.getArrayDataType().equals(type)) return Type.DECIMAL;
    if (SQLDataType.LONGVARCHAR.getArrayDataType().equals(type)) return Type.TEXT;
    if (SQLDataType.DATE.getArrayDataType().equals(type)) return Type.DATE;
    if (SQLDataType.TIMESTAMPWITHTIMEZONE.getArrayDataType().equals(type)) return Type.DATETIME;

    throw new UnsupportedOperationException(
        "Unsupported SQL type found:" + f.getDataType().getSQLType() + " " + f.getDataType());
  }

  public static Collection<Object> getValuesAsCollection(Row row, Table table)
      throws MolgenisException {
    Collection<Object> values = new ArrayList<>();
    for (Column c : table.getColumns()) {
      values.add(getTypedValue(row, c));
    }
    return values;
  }

  public static Object getTypedValue(Row row, Column column) throws MolgenisException {
    Type type = column.getType();
    if (Type.REF.equals(type)) {
      type =
          column
              .getTable()
              .getSchema()
              .getTable(column.getRefTable())
              .getColumn(column.getRefColumn())
              .getType();
      ;
    }
    if (Type.REF_ARRAY.equals(type)) {
      type =
          column
              .getTable()
              .getSchema()
              .getTable(column.getRefTable())
              .getColumn(column.getRefColumn())
              .getType();
      ;;
      switch (type) {
        case UUID:
          type = Type.UUID_ARRAY;
          break;
        case STRING:
          type = Type.STRING_ARRAY;
          break;
        case BOOL:
          type = Type.BOOL_ARRAY;
          break;
        case INT:
          type = Type.INT_ARRAY;
          break;
        case DECIMAL:
          type = Type.DECIMAL_ARRAY;
          break;
        case TEXT:
          type = Type.TEXT_ARRAY;
          break;
        case DATE:
          type = Type.DATE_ARRAY;
          break;
        case DATETIME:
          type = Type.DATETIME_ARRAY;
          break;
        default:
          throw new UnsupportedOperationException("Unsupported REF_ARRAY type found:" + type);
      }
    }
    switch (type) {
      case UUID:
        return row.getUuid(column.getName());
      case UUID_ARRAY:
        return row.getUuidArray(column.getName());
      case STRING:
        return row.getString(column.getName());
      case STRING_ARRAY:
        return row.getStringArray(column.getName());
      case BOOL:
        return row.getBool(column.getName());
      case BOOL_ARRAY:
        return row.getBoolArray(column.getName());
      case INT:
        return row.getInt(column.getName());
      case INT_ARRAY:
        return row.getIntArray(column.getName());
      case DECIMAL:
        return row.getDecimal(column.getName());
      case DECIMAL_ARRAY:
        return row.getDecimalArray(column.getName());
      case TEXT:
        return row.getText(column.getName());
      case TEXT_ARRAY:
        return row.getTextArray(column.getName());
      case DATE:
        return row.getDate(column.getName());
      case DATE_ARRAY:
        return row.getDateArray(column.getName());
      case DATETIME:
        return row.getDateTime(column.getName());
      case DATETIME_ARRAY:
        return row.getDateTimeArray(column.getName());
      default:
        throw new UnsupportedOperationException("Unsupported type found:" + column.getType());
    }
  }

  public static Type pslqToMolgenisType(String dataType) {
    switch (dataType) {
      case "character varying":
        return Type.STRING;
      case "uuid":
        return Type.UUID;
      case "bool":
        return Type.BOOL;
      case "integer":
        return Type.INT;
      case "decimal":
        return Type.DECIMAL;
      case "text":
        return Type.TEXT;
      case "date":
        return Type.DATE;
      case "timestamp without time zone":
        return Type.DATETIME;
      default:
        throw new RuntimeException("data type unknown " + dataType);
    }
  }

  public static String getPsqlType(Column column) {
    switch (column.getType()) {
      case STRING:
        return "character varying";
      case UUID:
        return "uuid";
      case BOOL:
        return "bool";
      case INT:
        return "int";
      case DECIMAL:
        return "decimal";
      case TEXT:
        return "text";
      case DATE:
        return "date";
      case DATETIME:
        return "timestamp without time zone";
      default:
        throw new RuntimeException("data cannot be mapped to psqlType " + column.getType());
    }
  }
}
