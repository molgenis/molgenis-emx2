package org.molgenis.emx2.io.parquet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimestampLogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

class RowGroupConverter extends GroupConverter {

  private final List<ColumnConverter> columnConverters = new ArrayList<>();
  private Row row;

  RowGroupConverter(MessageType schema) {
    for (Type field : schema.getFields()) {
      if (!field.isPrimitive()) {
        throw new MolgenisException(
            "Parquet column '" + field.getName() + "' is nested, which is not supported");
      }
      if (field.getLogicalTypeAnnotation() instanceof TimestampLogicalTypeAnnotation timestamp
          && timestamp.getUnit() != TimeUnit.MICROS) {
        throw new MolgenisException(
            "Parquet column '"
                + field.getName()
                + "' is a timestamp in "
                + timestamp.getUnit()
                + ", only timestamps in MICROS are supported");
      }
      columnConverters.add(
          new ColumnConverter(
              field.getName(),
              field.getLogicalTypeAnnotation(),
              field.isRepetition(Type.Repetition.REPEATED)));
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
    for (ColumnConverter columnConverter : columnConverters) {
      row.set(columnConverter.columnName, null);
      columnConverter.repeatedValues.clear();
    }
  }

  @Override
  public void end() {
    for (ColumnConverter columnConverter : columnConverters) {
      if (columnConverter.repeated && !columnConverter.repeatedValues.isEmpty()) {
        row.set(columnConverter.columnName, columnConverter.repeatedValuesAsArray());
      }
    }
  }

  private class ColumnConverter extends PrimitiveConverter {

    private final String columnName;
    private final LogicalTypeAnnotation logicalType;
    private final boolean repeated;
    private final List<Object> repeatedValues = new ArrayList<>();

    ColumnConverter(String columnName, LogicalTypeAnnotation logicalType, boolean repeated) {
      this.columnName = columnName;
      this.logicalType = logicalType;
      this.repeated = repeated;
    }

    private void set(Object value) {
      if (repeated) {
        repeatedValues.add(value);
      } else {
        row.set(columnName, value);
      }
    }

    private Object[] repeatedValuesAsArray() {
      Object[] array = new Object[repeatedValues.size()];
      return repeatedValues.toArray(array);
    }

    @Override
    public void addBinary(Binary value) {
      set(value.toStringUsingUTF8());
    }

    @Override
    public void addInt(int value) {
      if (logicalType instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
        set(LocalDate.ofEpochDay(value));
      } else {
        set(value);
      }
    }

    @Override
    public void addLong(long value) {
      if (logicalType instanceof TimestampLogicalTypeAnnotation) {
        set(ParquetTimestamp.toLocalDateTime(value));
      } else {
        set(value);
      }
    }

    @Override
    public void addFloat(float value) {
      set((double) value);
    }

    @Override
    public void addDouble(double value) {
      set(value);
    }

    @Override
    public void addBoolean(boolean value) {
      set(value);
    }
  }
}
