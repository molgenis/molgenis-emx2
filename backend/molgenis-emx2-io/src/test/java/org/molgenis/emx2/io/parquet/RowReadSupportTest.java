package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;

class RowReadSupportTest {

  @TempDir Path tmp;

  @Test
  void givenPrimitiveColumns_whenReading_thenValuesKeepTheirType() throws IOException {
    Schema schema =
        SchemaBuilder.record("Product")
            .fields()
            .requiredString("name")
            .requiredInt("stock")
            .requiredLong("barcode")
            .requiredDouble("price")
            .requiredBoolean("available")
            .endRecord();

    GenericRecord genericRecord = new GenericData.Record(schema);
    genericRecord.put("name", "Bread");
    genericRecord.put("stock", 12);
    genericRecord.put("barcode", 8710000000001L);
    genericRecord.put("price", 2.5);
    genericRecord.put("available", true);
    Path file = writeAvroFile(schema, genericRecord);

    Row row = readAll(file).getFirst();

    assertEquals("Bread", row.getValueMap().get("name"));
    assertEquals(12, row.getValueMap().get("stock"));
    assertEquals(8710000000001L, row.getValueMap().get("barcode"));
    assertEquals(2.5, row.getValueMap().get("price"));
    assertEquals(true, row.getValueMap().get("available"));
  }

  @Test
  void givenDateColumn_whenReading_thenValueIsLocalDate() throws IOException {
    Schema dateType = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
    Schema schema =
        SchemaBuilder.record("Product")
            .fields()
            .name("bakedOn")
            .type(dateType)
            .noDefault()
            .endRecord();
    GenericRecord genericRecord = new GenericData.Record(schema);
    genericRecord.put("bakedOn", (int) LocalDate.of(2026, 11, 16).toEpochDay());
    Path file = writeAvroFile(schema, genericRecord);

    Row row = readAll(file).getFirst();

    assertEquals(LocalDate.of(2026, 11, 16), row.getValueMap().get("bakedOn"));
  }

  @Test
  void givenNullValue_whenReading_thenColumnIsPresentAndNull() throws IOException {
    Schema schema =
        SchemaBuilder.record("Product")
            .fields()
            .requiredString("name")
            .optionalInt("price")
            .endRecord();
    GenericRecord genericRecord = new GenericData.Record(schema);
    genericRecord.put("name", "Bread");
    Path file = writeAvroFile(schema, genericRecord);

    CompareTools.assertEquals(List.of(Row.row("name", "Bread", "price", null)), readAll(file));
  }

  @Test
  void givenMultipleRows_whenReading_thenRowsDoNotShareValues() throws IOException {
    Schema schema =
        SchemaBuilder.record("Product")
            .fields()
            .requiredString("name")
            .optionalInt("price")
            .endRecord();
    GenericRecord bread = new GenericData.Record(schema);
    bread.put("name", "Bread");
    bread.put("price", 1);
    GenericRecord butter = new GenericData.Record(schema);
    butter.put("name", "Butter");
    Path file = writeAvroFile(schema, bread, butter);

    CompareTools.assertEquals(
        List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", null)),
        readAll(file));
  }

  @Test
  void givenNestedColumn_whenReading_thenThrow() throws IOException {
    Schema schema =
        SchemaBuilder.record("Product")
            .fields()
            .name("tags")
            .type()
            .array()
            .items()
            .stringType()
            .noDefault()
            .endRecord();
    GenericRecord genericRecord = new GenericData.Record(schema);
    genericRecord.put("tags", List.of("fresh"));
    Path file = writeAvroFile(schema, genericRecord);

    assertThrows(MolgenisException.class, () -> readAll(file));
  }

  private Path writeAvroFile(Schema schema, GenericRecord... records) throws IOException {
    Path file = tmp.resolve("product.parquet");
    try (ParquetWriter<GenericRecord> writer =
        AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(file))
            .withConf(new PlainParquetConfiguration())
            .withSchema(schema)
            .withDataModel(GenericData.get())
            .build()) {
      for (GenericRecord genericRecord : records) {
        writer.write(genericRecord);
      }
    }
    return file;
  }

  private static List<Row> readAll(Path file) throws IOException {
    List<Row> rows = new ArrayList<>();
    try (ParquetReader<Row> reader =
        new RowParquetReaderBuilder(new LocalInputFile(file), new PlainParquetConfiguration())
            .build()) {
      Row row;
      while ((row = reader.read()) != null) {
        rows.add(row);
      }
    }
    return rows;
  }
}
