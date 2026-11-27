package org.molgenis.emx2.io.parquet;

import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.OutputFile;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;

public class RowParquetWriterBuilder extends ParquetWriter.Builder<Row, RowParquetWriterBuilder> {

  private final List<String> columnNames;
  private final TableMetadata table;

  public RowParquetWriterBuilder(OutputFile file, TableMetadata table, List<String> columnNames) {
    super(file);
    this.table = table;
    this.columnNames = columnNames;
  }

  @Override
  protected RowParquetWriterBuilder self() {
    return this;
  }

  @Override
  protected WriteSupport<Row> getWriteSupport(Configuration configuration) {
    return new RowWriteSupport(table, columnNames);
  }

  @Override
  protected WriteSupport<Row> getWriteSupport(ParquetConfiguration configuration) {
    return new RowWriteSupport(table, columnNames);
  }
}
