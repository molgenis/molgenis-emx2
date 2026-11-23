package org.molgenis.emx2.io.parquet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;

public class RowWriteSupport extends WriteSupport<Row> {

  private final SequencedMap<String, ColumnType> columnTypes = new LinkedHashMap<>();
  private RecordConsumer recordConsumer;

  public RowWriteSupport(TableMetadata table, List<String> columnNames) {
    for (String columnName : columnNames) {
      columnTypes.put(columnName, toColumType(table, columnName));
    }
  }

  private static ColumnType toColumType(TableMetadata table, String columnName) {
    Column column = table.getColumn(columnName);
    if (column == null) {
      throw new MolgenisException(
          "Cannot write column '"
              + columnName
              + "' to Parquet: table '"
              + table.getTableName()
              + "' has no column with that name");
    }
    return column.getColumnType();
  }

  @Override
  public WriteContext init(Configuration configuration) {
    return writeContext();
  }

  @Override
  public WriteContext init(ParquetConfiguration configuration) {
    return writeContext();
  }

  private WriteContext writeContext() {
    return new WriteContext(ParquetTypeMapping.schema(columnTypes), Map.of());
  }

  @Override
  public void prepareForWrite(RecordConsumer recordConsumer) {
    this.recordConsumer = recordConsumer;
  }

  @Override
  public void write(Row row) {
    recordConsumer.startMessage();

    try {
      int index = 0;
      for (Map.Entry<String, ColumnType> column : columnTypes.entrySet()) {
        List<Object> values = valuesToWrite(row, column.getKey(), column.getValue());
        if (!values.isEmpty()) {
          writeField(column.getKey(), index, values);
        }
        index++;
      }
    } finally {
      recordConsumer.endMessage();
    }
  }

  private static List<Object> valuesToWrite(Row row, String columnName, ColumnType columnType) {
    if (row.isNull(columnName, columnType)) {
      return List.of();
    }

    if (ParquetTypeMapping.isRepeated(columnType)) {
      Object[] array = (Object[]) row.get(columnName, columnType);
      return Arrays.stream(array).filter(Objects::nonNull).toList();
    }

    return List.of(row.get(columnName, ParquetTypeMapping.valueType(columnType)));
  }

  private void writeField(String columnName, int index, List<Object> values) {
    recordConsumer.startField(columnName, index);
    for (Object value : values) {
      consumeValue(value);
    }
    recordConsumer.endField(columnName, index);
  }

  private void consumeValue(Object value) {
    switch (value) {
      case Boolean bool -> recordConsumer.addBoolean(bool);
      case Integer integer -> recordConsumer.addInteger(integer);
      case Long longValue -> recordConsumer.addLong(longValue);
      case Double decimal -> recordConsumer.addDouble(decimal);
      case LocalDate date -> recordConsumer.addInteger(Math.toIntExact(date.toEpochDay()));
      case LocalDateTime dateTime -> {
        long micros =
            dateTime.toEpochSecond(ZoneOffset.UTC) * 1_000_000L + dateTime.getNano() / 1_000L;
        recordConsumer.addLong(micros);
      }
      case String string -> recordConsumer.addBinary(Binary.fromString(string));
      default ->
          throw new MolgenisException(
              "Cannot write value of type " + value.getClass().getName() + " to Parquet");
    }
  }
}
