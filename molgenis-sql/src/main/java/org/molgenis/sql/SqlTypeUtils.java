package org.molgenis.sql;

import org.jooq.DataType;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.jooq.impl.DSL.cast;
import static org.jooq.impl.DSL.value;
import static org.molgenis.Type.MREF;
import static org.molgenis.Type.REF_ARRAY;

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
        return SQLDataType.CLOB;
      case TEXT_ARRAY:
        return SQLDataType.CLOB.getArrayDataType();
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

  public static Collection<Object> getValuesAsCollection(Row row, Table table)
      throws MolgenisException {
    Collection<Object> values = new ArrayList<>();
    for (Column c : table.getColumns()) {
      values.add(getTypedValue(row, c));
    }
    return values;
  }

  public static Object getTypedValue(Object v, Column column) throws MolgenisException {
    Type type = column.getType();
    if (Type.REF.equals(type)) {
      type = getRefType(column);
    }
    switch (type) {
      case UUID:
        return TypeUtils.toUuid(v);
      case UUID_ARRAY:
        return TypeUtils.toUuidArray(v);
      case STRING:
        return TypeUtils.toString(v);
      case STRING_ARRAY:
        return TypeUtils.toStringArray(v);
      case BOOL:
        return TypeUtils.toBool(v);
      case BOOL_ARRAY:
        return TypeUtils.toBoolArray(v);
      case INT:
        return TypeUtils.toInt(v);
      case INT_ARRAY:
        return TypeUtils.toIntArray(v);
      case DECIMAL:
        return TypeUtils.toDecimal(v);
      case DECIMAL_ARRAY:
        return TypeUtils.toDecimalArray(v);
      case TEXT:
        return cast(TypeUtils.toText(v), SQLDataType.CLOB);
      case TEXT_ARRAY:
        return cast(TypeUtils.toTextArray(v), SQLDataType.CLOB.getArrayDataType());
      case DATE:
        return TypeUtils.toDate(v);
      case DATE_ARRAY:
        return TypeUtils.toDateArrray(v);
      case DATETIME:
        return TypeUtils.toDateTime(v);
      case DATETIME_ARRAY:
        return TypeUtils.toDateTimeArray(v);
      default:
        throw new UnsupportedOperationException("Unsupported type type found:" + type);
    }
  }

  public static Type getRefType(Column column) throws MolgenisException {
    return column
        .getTable()
        .getSchema()
        .getTable(column.getRefTable())
        .getColumn(column.getRefColumn())
        .getType();
  }

  public static Type getArrayType(Type type) {
    switch (type) {
      case UUID:
        return Type.UUID_ARRAY;
      case STRING:
        return Type.STRING_ARRAY;
      case BOOL:
        return Type.BOOL_ARRAY;
      case INT:
        return Type.INT_ARRAY;
      case DECIMAL:
        return Type.DECIMAL_ARRAY;
      case TEXT:
        return Type.TEXT_ARRAY;
      case DATE:
        return Type.DATE_ARRAY;
      case DATETIME:
        return Type.DATETIME_ARRAY;
      default:
        throw new UnsupportedOperationException("Unsupported REF_ARRAY type found:" + type);
    }
  }

  public static Object getTypedValue(Row row, Column column) throws MolgenisException {
    Type type = column.getType();
    if (Type.REF.equals(type)) {
      type = getRefType(column);
    }
    if (REF_ARRAY.equals(type) || MREF.equals(type)) {
      type = getArrayType(getRefType(column));
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

  public static String getPsqlType(Column column) throws MolgenisException {
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
        throw new MolgenisException(
            "Internal: data cannot be mapped to psqlType " + column.getType());
    }
  }
}
