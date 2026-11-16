package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParquetReaderTest {

  @Test
  void readParquet() throws URISyntaxException, IOException {
    Path path =
        Path.of(
            Objects.requireNonNull(
                    getClass().getClassLoader().getResource("parquet/sample-users.parquet"))
                .toURI());

    try (ParquetReader<GenericRecord> recordReader = getRecordReader(path)) {
      GenericRecord record;
      while ((record = recordReader.read()) != null) {
        System.out.println(record);
      }
    }
  }

  private static ParquetReader<GenericRecord> getRecordReader(Path path) throws IOException {
    return AvroParquetReader.<GenericRecord>builder(
            new LocalInputFile(path), new PlainParquetConfiguration())
        .withDataModel(GenericData.get())
        .build();
  }

  @Test
  void writeParquet(@TempDir Path tmp) throws IOException {
    Path path = tmp.resolve("people-example.parquet");
    Schema schema =
        SchemaBuilder.record("Person")
            .fields()
            .requiredString("name")
            .optionalInt("age")
            .endRecord();

    try (ParquetWriter<GenericRecord> writer = getRecordWriter(path, schema)) {
      GenericRecord person = new GenericData.Record(schema);
      person.put("name", "Alice");
      person.put("age", 30);
      writer.write(person);
    }

    try (ParquetReader<GenericRecord> reader = getRecordReader(path)) {
      GenericRecord record = reader.read();
      assertEquals("Alice", record.get("name").toString());
      assertEquals(30, record.get("age"));
    }
  }

  private static ParquetWriter<GenericRecord> getRecordWriter(Path path, Schema schema)
      throws IOException {
    return AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(path))
        .withConf(new PlainParquetConfiguration())
        .withSchema(schema)
        .withDataModel(GenericData.get())
        .build();
  }
}
