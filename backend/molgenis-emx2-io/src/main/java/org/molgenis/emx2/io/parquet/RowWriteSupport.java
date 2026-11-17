package org.molgenis.emx2.io.parquet;

import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Types;
import org.molgenis.emx2.Row;

public class RowWriteSupport extends WriteSupport<Row> {

  private final List<String> columnNames;
  private RecordConsumer recordConsumer;

  public RowWriteSupport(List<String> columnNames) {
    this.columnNames = List.copyOf(columnNames);
  }

  @Override
  public WriteContext init(Configuration configuration) {
    return writeContext();
  }

  @Override
  public WriteContext init(ParquetConfiguration configuration) {
    return writeContext();
  }

  /**
   * Builds the file schema: one column per column name, in the same order as {@code columnNames}.
   * Every column is stored as text: {@code optional} so a missing value is written as null, and
   * {@code binary} with the string type so other tools read the bytes as UTF-8 text.
   *
   * <p>{@link #write(Row)} relies on this order: it writes the value of {@code columnNames.get(i)}
   * to field {@code i}.
   */
  private WriteContext writeContext() {
    Types.MessageTypeBuilder schema = Types.buildMessage();
    for (String columnName : columnNames) {
      schema
          .optional(PrimitiveTypeName.BINARY)
          .as(LogicalTypeAnnotation.stringType())
          .named(columnName);
    }
    return new WriteContext(schema.named("Row"), Map.of());
  }

  @Override
  public void prepareForWrite(RecordConsumer recordConsumer) {
    this.recordConsumer = recordConsumer;
  }

  @Override
  public void write(Row row) {
    recordConsumer.startMessage();

    try {
      for (int i = 0; i < columnNames.size(); i++) {
        String value = row.getString(columnNames.get(i));

        if (value != null) {
          recordConsumer.startField(columnNames.get(i), i);
          recordConsumer.addBinary(Binary.fromString(value));
          recordConsumer.endField(columnNames.get(i), i);
        }
      }
    } finally {
      recordConsumer.endMessage();
    }
  }
}
