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
  void givenMillis_whenConvertingToLocalDateTime_thenDateTimeIsReturned() {
    assertEquals(
        LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_000_000),
        ParquetTimestamp.toLocalDateTime(1_795_415_415_123L));
  }
}
