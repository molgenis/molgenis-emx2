package org.molgenis.emx2.sql;

import org.jooq.DataType;
import org.jooq.impl.SQLDataType;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.emx2.MolgenisException;

import java.util.ArrayList;
import java.util.Collection;

import static org.jooq.impl.DSL.cast;
import static org.molgenis.emx2.ColumnType.*;

public class SqlTypeUtils extends TypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType jooqTypeOf(Column column) {
    ColumnType sqlColumnType = column.getColumnType();
    switch (sqlColumnType) {
      case REF:
        return jooqTypeOf(column.getRefColumn());
      case REFBACK:
      case REF_ARRAY:
      case MREF:
        return jooqTypeOf(column.getRefColumn()).getArrayDataType();
      default:
        return jooqTypeOf(sqlColumnType);
    }
  }

  public static DataType jooqTypeOf(ColumnType columnType) {
    switch (columnType) {
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
        return SQLDataType.VARCHAR;
      case TEXT_ARRAY:
        return SQLDataType.VARCHAR.getArrayDataType();
      case DATE:
        return SQLDataType.DATE;
      case DATE_ARRAY:
        return SQLDataType.DATE.getArrayDataType();
      case DATETIME:
        return SQLDataType.TIMESTAMP;
      case DATETIME_ARRAY:
        return SQLDataType.TIMESTAMP.getArrayDataType();
      default:
        // should never happen
        throw new IllegalArgumentException("addColumn(name,type) : unsupported type " + columnType);
    }
  }

  public static Collection<Object> getValuesAsCollection(Row row, Table table) {
    Collection<Object> values = new ArrayList<>();
    for (Column c : table.getMetadata().getLocalColumns()) {
      // rls
      if (Constants.MG_EDIT_ROLE.equals(c.getName())) {
        // big todo if we want to allow usernames here or role names
        values.add(Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
      } else {
        values.add(getTypedValue(row, c));
      }
    }
    return values;
  }

  public static Object getTypedValue(Object v, Column column) {
    ColumnType columnType = column.getColumnType();
    if (REF.equals(columnType)) {
      columnType = getRefColumnType(column);
    } else if (MREF.equals(columnType) || REFBACK.equals(columnType)) {
      columnType = getArrayType(getRefColumnType(column));
    }
    return getTypedValue(v, columnType);
  }

  private static Object getTypedValue(Object v, ColumnType columnType) {
    switch (columnType) {
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
        return cast(TypeUtils.toText(v), SQLDataType.VARCHAR);
      case TEXT_ARRAY:
        return cast(TypeUtils.toTextArray(v), SQLDataType.VARCHAR.getArrayDataType());
      case DATE:
        return TypeUtils.toDate(v);
      case DATE_ARRAY:
        return TypeUtils.toDateArrray(v);
      case DATETIME:
        return TypeUtils.toDateTime(v);
      case DATETIME_ARRAY:
        return TypeUtils.toDateTimeArray(v);
      default:
        throw new UnsupportedOperationException(
            "Unsupported columnType columnType found:" + columnType);
    }
  }

  public static Object getTypedValue(Row row, Column column) {
    ColumnType columnType = column.getColumnType();

    if (REF.equals(columnType)) {
      columnType = getRefColumnType(column);
    }
    if (REF_ARRAY.equals(columnType) || MREF.equals(columnType) || REFBACK.equals(columnType)) {
      columnType = getRefArrayColumnType(column);
    }
    switch (columnType) {
      case UUID:
        return row.getUuid(column.getName());
      case UUID_ARRAY:
        return row.getUuidArray(column.getName());
      case STRING:
        return row.getString(column.getName());
      case STRING_ARRAY:
        return row.getStringArray(column.getName());
      case BOOL:
        return row.getBoolean(column.getName());
      case BOOL_ARRAY:
        return row.getBooleanArray(column.getName());
      case INT:
        return row.getInteger(column.getName());
      case INT_ARRAY:
        return row.getIntegerArray(column.getName());
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
        throw new UnsupportedOperationException("Unsupported columnType found:" + columnType);
    }
  }

  public static ColumnType getRefArrayColumnType(Column column) {
    return getArrayType(getRefColumnType(column));
  }

  public static ColumnType getRefColumnType(Column column) {
    ColumnType columnType;
    Column refColumn = column.getRefColumn();
    while (REF.equals(refColumn.getColumnType()) || REF_ARRAY.equals(refColumn.getColumnType())) {
      refColumn = refColumn.getRefColumn();
      // check self reference
      if (refColumn.getTableName().equals(column.getTableName())
          && refColumn.getName().equals(column.getName())) {
        return STRING;
      }
    }
    columnType = refColumn.getColumnType();
    return columnType;
  }

  //  public static Column getRefColumn(Column column) {
  //    TableMetadata refTable = getRefTable(column);
  //    if (column.getRefColumnName() == null) {
  //      return refTable.getPrimaryKeyColumn();
  //    } else {
  //      return refTable.getColumn(column.getRefColumnName());
  //    }
  //  }

  public static TableMetadata getRefTable(Column column) {
    return column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  public static String getPsqlType(Column column) {
    switch (column.getColumnType()) {
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
        return "character varying";
      case DATE:
        return "date";
      case DATETIME:
        return "timestamp without time zone";
      case REF:
      case REF_ARRAY:
        return getPsqlType(column.getRefColumn());
      default:
        throw new MolgenisException(
            "Unknown type",
            "Internal error: data cannot be mapped to psqlType " + column.getColumnType());
    }
  }
}
