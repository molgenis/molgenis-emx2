package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;

class RowRecordConversionTest {

  @Test
  void givenFieldWithColumnNameProperty_whenConvertingToRow_thenUseOriginalName() {
    Schema schema =
        SchemaBuilder.record("Product")
            .fields()
            .name("product_name")
            .prop(RowRecordConversion.COLUMN_NAME_PROPERTY, "product name")
            .type()
            .optional()
            .stringType()
            .endRecord();
    GenericRecord record = new GenericData.Record(schema);
    record.put("product_name", "Bread");

    Row row = RowRecordConversion.toRow(record);

    CompareTools.assertEquals(Row.row("product name", "Bread"), row);
  }

  @Test
  void givenFieldWithoutColumnNameProperty_whenConvertingToRow_thenUseFieldName() {
    Schema schema = SchemaBuilder.record("Product").fields().optionalString("name").endRecord();
    GenericRecord record = new GenericData.Record(schema);
    record.put("name", "Bread");

    Row row = RowRecordConversion.toRow(record);

    CompareTools.assertEquals(Row.row("name", "Bread"), row);
  }

  @Test
  void givenUtf8Value_whenConvertingToRow_thenValueIsString() {
    Schema schema = SchemaBuilder.record("Product").fields().optionalString("name").endRecord();
    GenericRecord record = new GenericData.Record(schema);
    record.put("name", new Utf8("Bread"));

    Row row = RowRecordConversion.toRow(record);

    assertEquals("Bread", row.getValueMap().get("name"));
  }

  @Test
  void givenNullValue_whenConvertingToRow_thenColumnIsNull() {
    Schema schema = SchemaBuilder.record("Product").fields().optionalInt("price").endRecord();
    GenericRecord record = new GenericData.Record(schema);

    Row row = RowRecordConversion.toRow(record);

    CompareTools.assertEquals(Row.row("price", null), row);
  }
}
