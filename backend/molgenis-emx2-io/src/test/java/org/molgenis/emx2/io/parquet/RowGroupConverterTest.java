package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Types;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;

class RowGroupConverterTest {

  @Nested
  class Schema {

    @Test
    void givenNestedColumn_whenCreatingConverter_thenThrow() {
      MessageType schema =
          Types.buildMessage()
              .optionalGroup()
              .optional(PrimitiveTypeName.BINARY)
              .named("street")
              .named("address")
              .named("Row");

      MolgenisException exception =
          assertThrows(MolgenisException.class, () -> new RowGroupConverter(schema));
      assertTrue(exception.getMessage().contains("address"));
    }

    @ParameterizedTest
    @EnumSource(
        value = TimeUnit.class,
        names = {"MICROS", "NANOS"})
    void givenTimestampNotInMillis_whenCreatingConverter_thenThrow(TimeUnit unit) {
      MessageType schema =
          Types.buildMessage()
              .optional(PrimitiveTypeName.INT64)
              .as(LogicalTypeAnnotation.timestampType(false, unit))
              .named("bakedAt")
              .named("Row");

      MolgenisException exception =
          assertThrows(MolgenisException.class, () -> new RowGroupConverter(schema));
      assertEquals(
          "Parquet column 'bakedAt' is a timestamp in "
              + unit
              + ", only timestamps in MILLIS are supported",
          exception.getMessage());
    }

    @Test
    void givenColumns_whenConvertingValue_thenValueGoesToColumnAtIndex() {
      RowGroupConverter converter =
          new RowGroupConverter(
              Types.buildMessage()
                  .optional(PrimitiveTypeName.INT32)
                  .named("stock")
                  .optional(PrimitiveTypeName.INT32)
                  .named("price")
                  .named("Row"));

      converter.start();
      column(converter, 1).addInt(3);
      converter.end();

      CompareTools.assertEquals(Row.row("stock", null, "price", 3), converter.getCurrentRow());
    }
  }

  @Nested
  class Start {

    @Test
    void givenColumns_whenStartingRow_thenAllColumnsAreNull() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.BINARY, "name");

      converter.start();
      converter.end();

      CompareTools.assertEquals(Row.row("name", null), converter.getCurrentRow());
    }

    @Test
    void givenPreviousRow_whenStartingNextRow_thenPreviousRowIsUnchanged() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.INT32, "price");

      converter.start();
      column(converter, 0).addInt(1);
      converter.end();
      Row first = converter.getCurrentRow();

      converter.start();
      converter.end();
      Row second = converter.getCurrentRow();

      CompareTools.assertEquals(Row.row("price", 1), first);
      CompareTools.assertEquals(Row.row("price", null), second);
    }
  }

  @Nested
  class Values {

    @Test
    void givenBinary_whenConverting_thenValueIsString() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.BINARY, "name");

      Object value = convert(converter, column -> column.addBinary(Binary.fromString("Bread")));

      assertEquals("Bread", value);
    }

    @Test
    void givenInt_whenConverting_thenValueIsInteger() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.INT32, "stock");

      Object value = convert(converter, column -> column.addInt(12));

      assertEquals(12, value);
    }

    @Test
    void givenIntWithDateType_whenConverting_thenValueIsLocalDate() {
      RowGroupConverter converter =
          new RowGroupConverter(
              Types.buildMessage()
                  .optional(PrimitiveTypeName.INT32)
                  .as(LogicalTypeAnnotation.dateType())
                  .named("bakedOn")
                  .named("Row"));
      int days = (int) LocalDate.of(2026, 11, 16).toEpochDay();

      Object value = convert(converter, column -> column.addInt(days));

      assertEquals(LocalDate.of(2026, 11, 16), value);
    }

    @Test
    void givenLong_whenConverting_thenValueIsLong() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.INT64, "barcode");

      Object value = convert(converter, column -> column.addLong(8710000000001L));

      assertEquals(8710000000001L, value);
    }

    @Test
    void givenLongWithLocalTimestampType_whenConverting_thenValueIsLocalDateTime() {
      RowGroupConverter converter =
          new RowGroupConverter(
              Types.buildMessage()
                  .optional(PrimitiveTypeName.INT64)
                  .as(LogicalTypeAnnotation.timestampType(false, TimeUnit.MILLIS))
                  .named("bakedAt")
                  .named("Row"));

      Object value = convert(converter, column -> column.addLong(1_795_415_415_123L));

      assertEquals(LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_000_000), value);
    }

    @Test
    void givenFloat_whenConverting_thenValueIsDouble() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.FLOAT, "price");

      Object value = convert(converter, column -> column.addFloat(2.5f));

      assertEquals(2.5, value);
    }

    @Test
    void givenDouble_whenConverting_thenValueIsDouble() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.DOUBLE, "price");

      Object value = convert(converter, column -> column.addDouble(2.5));

      assertEquals(2.5, value);
    }

    @Test
    void givenBoolean_whenConverting_thenValueIsBoolean() {
      RowGroupConverter converter = converterFor(PrimitiveTypeName.BOOLEAN, "available");

      Object value = convert(converter, column -> column.addBoolean(true));

      assertEquals(true, value);
    }
  }

  @Nested
  class Repeated {

    @Test
    void givenRepeatedValues_whenConverting_thenValueIsArray() {
      RowGroupConverter converter = repeatedConverterFor(PrimitiveTypeName.INT32, "batches");

      Object value =
          convert(
              converter,
              column -> {
                column.addInt(1);
                column.addInt(2);
                column.addInt(3);
              });

      assertArrayEquals(new Integer[] {1, 2, 3}, (Object[]) value);
    }

    @Test
    void givenRepeatedColumnWithoutValues_whenConverting_thenValueIsNull() {
      RowGroupConverter converter = repeatedConverterFor(PrimitiveTypeName.INT32, "batches");

      Object value = convert(converter, column -> {});

      assertNull(value);
    }

    @Test
    void givenPreviousRow_whenConvertingNextRow_thenValuesAreNotCarriedOver() {
      RowGroupConverter converter = repeatedConverterFor(PrimitiveTypeName.INT32, "batches");

      convert(converter, column -> column.addInt(1));
      Object value = convert(converter, column -> column.addInt(2));

      assertArrayEquals(new Integer[] {2}, (Object[]) value);
    }
  }

  private static RowGroupConverter repeatedConverterFor(PrimitiveTypeName type, String columnName) {
    return new RowGroupConverter(
        Types.buildMessage().repeated(type).named(columnName).named("Row"));
  }

  private static RowGroupConverter converterFor(PrimitiveTypeName type, String columnName) {
    return new RowGroupConverter(
        Types.buildMessage().optional(type).named(columnName).named("Row"));
  }

  private static PrimitiveConverter column(RowGroupConverter converter, int index) {
    return converter.getConverter(index).asPrimitiveConverter();
  }

  private static Object convert(
      RowGroupConverter converter, Consumer<PrimitiveConverter> addValue) {
    converter.start();
    addValue.accept(column(converter, 0));
    converter.end();
    return converter.getCurrentRow().getValueMap().values().iterator().next();
  }
}
