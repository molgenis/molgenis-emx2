package org.molgenis.emx2.io.parquet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;

/**
 * EMX2 date times have no zone, so they are written as Parquet timestamps that are not adjusted to
 * UTC: the number of milliseconds between 1970-01-01T00:00 and the date-time, both read as local
 * time. Reading a value back gives the written date-time, whatever zone the reader runs in.
 */
final class ParquetTimestamp {

  private static final LogicalTypeAnnotation LOGICAL_TYPE =
      LogicalTypeAnnotation.timestampType(false, TimeUnit.MILLIS);

  private ParquetTimestamp() {
    // Hide public constructor
  }

  static LogicalTypeAnnotation logicalType() {
    return LOGICAL_TYPE;
  }

  static long toMillis(LocalDateTime dateTime) {
    return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
  }

  static LocalDateTime toLocalDateTime(long millis) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
  }
}
