package org.molgenis.emx2.io.parquet;

import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.InputFile;
import org.molgenis.emx2.Row;

public class RowParquetReaderBuilder extends ParquetReader.Builder<Row> {

  public RowParquetReaderBuilder(InputFile file, ParquetConfiguration configuration) {
    super(file, configuration);
  }

  @Override
  protected ReadSupport<Row> getReadSupport() {
    return new RowReadSupport();
  }
}
