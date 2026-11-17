package org.molgenis.emx2.io.parquet;

import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.hadoop.api.InitContext;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;
import org.molgenis.emx2.Row;

public class RowReadSupport extends ReadSupport<Row> {

  @Override
  public ReadContext init(InitContext context) {
    return new ReadContext(context.getFileSchema());
  }

  @Override
  public RecordMaterializer<Row> prepareForRead(
      Configuration configuration,
      Map<String, String> keyValueMetaData,
      MessageType fileSchema,
      ReadContext readContext) {
    return rowMaterializer(readContext.getRequestedSchema());
  }

  @Override
  public RecordMaterializer<Row> prepareForRead(
      ParquetConfiguration configuration,
      Map<String, String> keyValueMetaData,
      MessageType fileSchema,
      ReadContext readContext) {
    return rowMaterializer(readContext.getRequestedSchema());
  }

  private static RecordMaterializer<Row> rowMaterializer(MessageType schema) {
    return new RecordMaterializer<>() {

      private final RowGroupConverter rowGroupConverter = new RowGroupConverter(schema);

      @Override
      public Row getCurrentRecord() {
        return rowGroupConverter.getCurrentRow();
      }

      @Override
      public GroupConverter getRootConverter() {
        return rowGroupConverter;
      }
    };
  }
}
