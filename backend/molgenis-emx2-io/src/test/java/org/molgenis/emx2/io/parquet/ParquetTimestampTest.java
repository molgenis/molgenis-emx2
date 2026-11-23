package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;
import org.junit.jupiter.api.Test;

class ParquetTimestampTest {

  @Test
  void givenLogicalType_thenTimestampIsLocalInMicroseconds() {
    assertEquals(
        LogicalTypeAnnotation.timestampType(false, TimeUnit.MICROS),
        ParquetTimestamp.logicalType());
  }

  @Test
  void givenDateTime_whenConvertingToMicros_thenMicrosSinceEpochAreReturned() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_456_000);

    assertEquals(1_795_415_415_123_456L, ParquetTimestamp.toMicros(dateTime));
  }

  @Test
  void givenDateTimeBefore1970_whenConvertingToMicros_thenMicrosAreNegative() {
    LocalDateTime dateTime = LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_000);

    assertEquals(-1L, ParquetTimestamp.toMicros(dateTime));
  }

  @Test
  void givenDateTimeWithNanos_whenConvertingToMicros_thenNanosAreDropped() {
    LocalDateTime dateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 1_999);

    assertEquals(1L, ParquetTimestamp.toMicros(dateTime));
  }

  @Test
  void givenMicros_whenConvertingToLocalDateTime_thenDateTimeIsReturned() {
    assertEquals(
        LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_456_000),
        ParquetTimestamp.toLocalDateTime(1_795_415_415_123_456L));
  }

  @Test
  void givenNegativeMicros_whenConvertingToLocalDateTime_thenDateTimeIsBefore1970() {
    assertEquals(
        LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_000),
        ParquetTimestamp.toLocalDateTime(-1L));
  }

  @Test
  void givenDateTime_whenConvertingToMicrosAndBack_thenDateTimeIsEqual() {
    LocalDateTime dateTime = LocalDateTime.of(1900, 2, 28, 13, 45, 0, 500_000);

    long micros = ParquetTimestamp.toMicros(dateTime);

    assertEquals(dateTime, ParquetTimestamp.toLocalDateTime(micros));
  }
}
