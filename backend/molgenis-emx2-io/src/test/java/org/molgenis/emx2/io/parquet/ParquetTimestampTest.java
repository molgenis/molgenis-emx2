package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;
import org.junit.jupiter.api.Test;

class ParquetTimestampTest {

  @Test
  void givenLogicalType_thenTimestampIsLocalInMilliseconds() {
    assertEquals(
        LogicalTypeAnnotation.timestampType(false, TimeUnit.MILLIS),
        ParquetTimestamp.logicalType());
  }

  @Test
  void givenDateTime_whenConvertingToMillis_thenMillisSinceEpochAreReturned() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_000_000);

    assertEquals(1_795_415_415_123L, ParquetTimestamp.toMillis(dateTime));
  }

  @Test
  void givenDateTimeBefore1970_whenConvertingToMillis_thenMillisAreNegative() {
    LocalDateTime dateTime = LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000);

    assertEquals(-1L, ParquetTimestamp.toMillis(dateTime));
  }

  @Test
  void givenDateTimeWithMicros_whenConvertingToMillis_thenMicrosAreDropped() {
    LocalDateTime dateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 1_999_000);

    assertEquals(1L, ParquetTimestamp.toMillis(dateTime));
  }

  @Test
  void givenMillis_whenConvertingToLocalDateTime_thenDateTimeIsReturned() {
    assertEquals(
        LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_000_000),
        ParquetTimestamp.toLocalDateTime(1_795_415_415_123L));
  }

  @Test
  void givenNegativeMillis_whenConvertingToLocalDateTime_thenDateTimeIsBefore1970() {
    assertEquals(
        LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000),
        ParquetTimestamp.toLocalDateTime(-1L));
  }

  @Test
  void givenDateTime_whenConvertingToMillisAndBack_thenDateTimeIsEqual() {
    LocalDateTime dateTime = LocalDateTime.of(1900, 2, 28, 13, 45, 0, 500_000_000);

    long millis = ParquetTimestamp.toMillis(dateTime);

    assertEquals(dateTime, ParquetTimestamp.toLocalDateTime(millis));
  }
}
