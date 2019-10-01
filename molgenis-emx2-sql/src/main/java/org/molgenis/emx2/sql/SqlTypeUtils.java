package org.molgenis.emx2.sql;

import org.jooq.DataType;
import org.jooq.impl.SQLDataType;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.Collection;

import static org.jooq.impl.DSL.cast;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;

public class SqlTypeUtils extends TypeUtils {

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  public static DataType jooqTypeOf(Column column) throws MolgenisException {
    ColumnType sqlColumnType = column.getColumnType();
    switch (sqlColumnType) {
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
      case REF:
        return jooqTypeOf(
            column
                .getTable()
                .getSchema()
                .getTableMetadata(column.getRefTableName())
                .getColumn(column.getRefColumnName()));
      case REF_ARRAY:
        return jooqTypeOf(
                column
                    .getTable()
                    .getSchema()
                    .getTableMetadata(column.getRefTableName())
                    .getColumn(column.getRefColumnName()))
            .getArrayDataType();
      case MREF:
        return jooqTypeOf(
                column
                    .getTable()
                    .getSchema()
                    .getTableMetadata(column.getRefTableName())
                    .getColumn(column.getRefColumnName()))
            .getArrayDataType();
      default:
        // should never happen
        throw new IllegalArgumentException(
            "addColumn(name,type) : unsupported type " + sqlColumnType);
    }
  }

  public static Collection<Object> getValuesAsCollection(Row row, Table table)
      throws MolgenisException {
    Collection<Object> values = new ArrayList<>();
    for (Column c : table.getMetadata().getColumns()) {
      // rls
      if (SqlTable.MG_EDIT_ROLE.equals(c.getColumnName())) {
        // big todo if we want to allow usernames here or role names
        values.add(SqlTable.MG_USER_PREFIX + row.getString(SqlTable.MG_EDIT_ROLE));
      } else {
        values.add(getTypedValue(row, c));
      }
    }
    return values;
  }

  public static Object getTypedValue(Object v, Column column) throws MolgenisException {
    ColumnType columnType = column.getColumnType();
    if (ColumnType.REF.equals(columnType)) {
      columnType = getRefType(column);
    }
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

  public static ColumnType getRefType(Column column) throws MolgenisException {
    return column
        .getTable()
        .getSchema()
        .getTableMetadata(column.getRefTableName())
        .getColumn(column.getRefColumnName())
        .getColumnType();
  }

  public static Object getTypedValue(Row row, Column column) throws MolgenisException {
    ColumnType columnType = column.getColumnType();
    if (ColumnType.REF.equals(columnType)) {
      columnType = getRefType(column);
    }
    if (REF_ARRAY.equals(columnType) || MREF.equals(columnType)) {
      columnType = getArrayType(getRefType(column));
    }
    switch (columnType) {
      case UUID:
        return row.getUuid(column.getColumnName());
      case UUID_ARRAY:
        return row.getUuidArray(column.getColumnName());
      case STRING:
        return row.getString(column.getColumnName());
      case STRING_ARRAY:
        return row.getStringArray(column.getColumnName());
      case BOOL:
        return row.getBoolean(column.getColumnName());
      case BOOL_ARRAY:
        return row.getBooleanArray(column.getColumnName());
      case INT:
        return row.getInteger(column.getColumnName());
      case INT_ARRAY:
        return row.getIntegerArray(column.getColumnName());
      case DECIMAL:
        return row.getDecimal(column.getColumnName());
      case DECIMAL_ARRAY:
        return row.getDecimalArray(column.getColumnName());
      case TEXT:
        return row.getText(column.getColumnName());
      case TEXT_ARRAY:
        return row.getTextArray(column.getColumnName());
      case DATE:
        return row.getDate(column.getColumnName());
      case DATE_ARRAY:
        return row.getDateArray(column.getColumnName());
      case DATETIME:
        return row.getDateTime(column.getColumnName());
      case DATETIME_ARRAY:
        return row.getDateTimeArray(column.getColumnName());
      default:
        throw new UnsupportedOperationException(
            "Unsupported columnType found:" + column.getColumnType());
    }
  }

  public static String getPsqlType(Column column) throws MolgenisException {
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
      default:
        throw new MolgenisException(
            "internal_error",
            "Should only happen during development",
            "Internal error: data cannot be mapped to psqlType " + column.getColumnType());
    }
  }
}
