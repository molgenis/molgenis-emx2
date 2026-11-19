package org.molgenis.emx2.io.parquet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

class RowGroupConverter extends GroupConverter {

  private final List<String> columnNames = new ArrayList<>();
  private final List<Converter> columnConverters = new ArrayList<>();
  private Row row;

  RowGroupConverter(MessageType schema) {
    for (Type field : schema.getFields()) {
      if (!field.isPrimitive()) {
        throw new MolgenisException(
            "Parquet column '" + field.getName() + "' is nested, which is not supported");
      }
      columnNames.add(field.getName());
      columnConverters.add(new ColumnConverter(field.getName(), field.getLogicalTypeAnnotation()));
    }
  }

  Row getCurrentRow() {
    return row;
  }

  @Override
  public Converter getConverter(int fieldIndex) {
    return columnConverters.get(fieldIndex);
  }

  @Override
  public void start() {
    row = new Row();
    for (String columnName : columnNames) {
      row.set(columnName, null);
    }
  }

  @Override
  public void end() {
    // no-op
  }

  private class ColumnConverter extends PrimitiveConverter {

    private final String columnName;
    private final LogicalTypeAnnotation logicalType;

    ColumnConverter(String columnName, LogicalTypeAnnotation logicalType) {
      this.columnName = columnName;
      this.logicalType = logicalType;
    }

    @Override
    public void addBinary(Binary value) {
      row.set(columnName, value.toStringUsingUTF8());
    }

    @Override
    public void addInt(int value) {
      if (logicalType instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
        row.set(columnName, LocalDate.ofEpochDay(value));
      } else {
        row.set(columnName, value);
      }
    }

    @Override
    public void addLong(long value) {
      row.set(columnName, value);
    }

    @Override
    public void addFloat(float value) {
      row.set(columnName, (double) value);
    }

    @Override
    public void addDouble(double value) {
      row.set(columnName, value);
    }

    @Override
    public void addBoolean(boolean value) {
      row.set(columnName, value);
    }
  }
}
