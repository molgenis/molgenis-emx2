package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Row;

class RowWriteSupportTest {

  @TempDir Path tmp;

  @Test
  void givenColumnNameWithSpace_whenWriting_thenFileKeepsTheName() throws IOException {
    Path file = write(List.of("product name"), List.of(Row.row("product name", "Bread")));

    assertEquals(List.of("product name"), fileSchemaColumnNames(file));
  }

  @Test
  void givenColumns_whenWriting_thenColumnsAreOptionalStrings() throws IOException {
    Path file = write(List.of("name"), List.of(Row.row("name", "Bread")));

    Type column = fileSchema(file).getType("name");
    assertEquals(Type.Repetition.OPTIONAL, column.getRepetition());
    assertEquals(PrimitiveTypeName.BINARY, column.asPrimitiveType().getPrimitiveTypeName());
    assertEquals(LogicalTypeAnnotation.stringType(), column.getLogicalTypeAnnotation());
  }

  private Path write(List<String> columnNames, List<Row> rows) throws IOException {
    Path file = tmp.resolve("product.parquet");
    try (ParquetWriter<Row> writer =
        new RowParquetWriterBuilder(new LocalOutputFile(file), columnNames)
            .withConf(new PlainParquetConfiguration())
            .build()) {
      for (Row row : rows) {
        writer.write(row);
      }
    }
    return file;
  }

  private static MessageType fileSchema(Path file) throws IOException {
    ParquetReadOptions options =
        ParquetReadOptions.builder(new PlainParquetConfiguration()).build();
    try (ParquetFileReader reader = ParquetFileReader.open(new LocalInputFile(file), options)) {
      return reader.getFooter().getFileMetaData().getSchema();
    }
  }

  private static List<String> fileSchemaColumnNames(Path file) throws IOException {
    return fileSchema(file).getFields().stream().map(Type::getName).toList();
  }
}
