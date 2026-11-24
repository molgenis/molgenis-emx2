package org.molgenis.emx2.io.parquet;

import static org.molgenis.emx2.Column.column;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.util.CompareTools;

class RowParquetRoundTripTest {

  private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
  private static final UUID RANDOM_UUID = UUID.randomUUID();

  private static final TableMetadata PRODUCTS =
      new TableMetadata("products")
          .add(
              column("name").setType(ColumnType.STRING),
              column("price").setType(ColumnType.DECIMAL),
              column("stock").setType(ColumnType.INT));

  @TempDir Path tmp;

  static Stream<Arguments> valuesPerColumnType() {
    return Stream.of(
        Arguments.of(ColumnType.BOOL, true),
        Arguments.of(ColumnType.BOOL_ARRAY, new Boolean[] {true, false}),
        Arguments.of(ColumnType.UUID, RANDOM_UUID),
        Arguments.of(ColumnType.UUID_ARRAY, new UUID[] {RANDOM_UUID}),
        Arguments.of(ColumnType.STRING, "Bread"),
        Arguments.of(ColumnType.FILE, "test.jpg"),
        Arguments.of(ColumnType.STRING_ARRAY, new String[] {"Bread", "Butter"}),
        Arguments.of(ColumnType.STRING_ARRAY, new String[] {"salt, sea", "flour"}),
        Arguments.of(ColumnType.JSON, JSONB.valueOf("{\"name\":\"Bread\"}")),
        Arguments.of(ColumnType.INT, 12),
        Arguments.of(ColumnType.INT_ARRAY, new Integer[] {1, 2}),
        Arguments.of(ColumnType.LONG, 8710000000001L),
        Arguments.of(ColumnType.LONG_ARRAY, new Long[] {8710000000001L, 8710000000002L}),
        Arguments.of(ColumnType.DECIMAL, 2.5),
        Arguments.of(ColumnType.DECIMAL_ARRAY, new Double[] {2.5, 3.75}),
        Arguments.of(ColumnType.DATE, LocalDate.of(2026, 11, 16)),
        Arguments.of(ColumnType.DATE_ARRAY, new LocalDate[] {LocalDate.of(2026, 11, 16)}),
        Arguments.of(ColumnType.DATETIME, NOW),
        Arguments.of(ColumnType.DATETIME_ARRAY, new LocalDateTime[] {NOW}),
        Arguments.of(ColumnType.PERIOD, Period.ofMonths(3)),
        Arguments.of(ColumnType.PERIOD_ARRAY, new Period[] {Period.ofMonths(3), Period.ofDays(5)}));
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("valuesPerColumnType")
  void givenValueOfColumnType_whenWritingAndReadingBack_thenValueIsEqual(
      ColumnType columnType, Object rawValue) throws IOException {
    TableMetadata table = new TableMetadata("values").add(column("value").setType(columnType));
    Row row = Row.row("value", rawValue);

    Path file = write(table, List.of("value"), List.of(row));

    CompareTools.assertEquals(List.of(row), readAll(file));
  }

  @Test
  void givenRowMissingColumn_whenWriting_thenValueIsNull() throws IOException {
    Path file =
        write(
            PRODUCTS,
            List.of("name", "price", "stock"),
            List.of(Row.row("name", "Bread", "stock", 42)));

    CompareTools.assertEquals(
        List.of(Row.row("name", "Bread", "price", null, "stock", 42)), readAll(file));
  }

  @Test
  void givenRowWithExtraColumn_whenWriting_thenExtraColumnIsDropped() throws IOException {
    Path file = write(PRODUCTS, List.of("name"), List.of(Row.row("name", "Bread", "price", 1)));

    CompareTools.assertEquals(List.of(Row.row("name", "Bread")), readAll(file));
  }

  private Path write(TableMetadata table, List<String> columnNames, List<Row> rows)
      throws IOException {
    Path file = tmp.resolve("product.parquet");
    try (ParquetWriter<Row> writer =
        new RowParquetWriterBuilder(new LocalOutputFile(file), table, columnNames)
            .withConf(new PlainParquetConfiguration())
            .build()) {
      for (Row row : rows) {
        writer.write(row);
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
