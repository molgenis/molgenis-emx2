package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;

class RowWriteSupportTest {

  @TempDir Path tmp;

  TableMetadata table =
      new TableMetadata("products")
          .add(
              Column.column("string").setType(ColumnType.STRING),
              Column.column("int").setType(ColumnType.INT),
              Column.column("decimal").setType(ColumnType.DECIMAL),
              Column.column("long").setType(ColumnType.LONG),
              Column.column("bool").setType(ColumnType.BOOL),
              Column.column("date").setType(ColumnType.DATE),
              Column.column("datetime").setType(ColumnType.DATETIME),
              Column.column("non_negative_int").setType(ColumnType.NON_NEGATIVE_INT),
              Column.column("string_array").setType(ColumnType.STRING_ARRAY),
              Column.column("int_array").setType(ColumnType.INT_ARRAY),
              Column.column("long_array").setType(ColumnType.LONG_ARRAY),
              Column.column("decimal_array").setType(ColumnType.DECIMAL_ARRAY),
              Column.column("bool_array").setType(ColumnType.BOOL_ARRAY),
              Column.column("date_array").setType(ColumnType.DATE_ARRAY),
              Column.column("datetime_array").setType(ColumnType.DATETIME_ARRAY),
              Column.column("uuid_array").setType(ColumnType.UUID_ARRAY),
              Column.column("file").setType(ColumnType.FILE));

  @Test
  void givenStringColumn_whenWriting_thenColumnIsOptionalString() throws IOException {
    Path file = write(List.of("string"), List.of(Row.row("string", "Bread")));

    Type column = fileSchema(file).getType("string");
    assertEquals(Type.Repetition.OPTIONAL, column.getRepetition());
    assertEquals(PrimitiveTypeName.BINARY, column.asPrimitiveType().getPrimitiveTypeName());
    assertEquals(LogicalTypeAnnotation.stringType(), column.getLogicalTypeAnnotation());
  }

  @Test
  void givenFileColumn_whenWritingWithFilename_thenBothAreStringColumns() throws IOException {
    Path file =
        write(
            List.of("file", "file_filename"),
            List.of(Row.row("file", "a1b2c3", "file_filename", "bread.jpg")));

    MessageType schema = fileSchema(file);
    assertPrimitive(schema, "file", PrimitiveTypeName.BINARY, LogicalTypeAnnotation.stringType());
    assertPrimitive(
        schema, "file_filename", PrimitiveTypeName.BINARY, LogicalTypeAnnotation.stringType());
    Group group = readRows(file).getFirst();
    assertEquals("a1b2c3", group.getString("file", 0));
    assertEquals("bread.jpg", group.getString("file_filename", 0));
  }

  @Test
  void givenSubtypeOfInt_whenWriting_thenColumnIsInt32() throws IOException {
    Path file = write(List.of("non_negative_int"), List.of(Row.row("non_negative_int", 2)));

    assertPrimitive(fileSchema(file), "non_negative_int", PrimitiveTypeName.INT32, null);
  }

  @Test
  void givenArrayOfTypeWithoutParquetCounterpart_whenWriting_thenColumnIsString()
      throws IOException {
    UUID uuid = UUID.randomUUID();

    Path file = write(List.of("uuid_array"), List.of(Row.row("uuid_array", new UUID[] {uuid})));

    assertPrimitive(
        fileSchema(file),
        "uuid_array",
        PrimitiveTypeName.BINARY,
        LogicalTypeAnnotation.stringType());
    assertEquals(uuid.toString(), readRows(file).getFirst().getString("uuid_array", 0));
  }

  static Stream<Arguments> writtenValuesAndStoredValues() {
    LocalDate date = LocalDate.of(2026, 11, 30);
    int epochDay = Math.toIntExact(date.toEpochDay());

    LocalDateTime dateTime = LocalDateTime.of(2026, 11, 23, 6, 30, 15, 123_456_000);
    long millis = 1_795_415_415_123L;

    return Stream.of(
        Arguments.of("string", "Bread", List.of("Bread")),
        Arguments.of("int", 3, List.of(3)),
        Arguments.of("long", 10_000_000_000L, List.of(10_000_000_000L)),
        Arguments.of("decimal", 2.49, List.of(2.49)),
        Arguments.of("bool", true, List.of(true)),
        Arguments.of("date", date, List.of(epochDay)),
        Arguments.of("datetime", dateTime, List.of(millis)),
        Arguments.of(
            "string_array",
            new String[] {"fresh", "local, organic"},
            List.of("fresh", "local, organic")),
        Arguments.of("int_array", new Integer[] {1, 2, 3}, List.of(1, 2, 3)),
        Arguments.of("long_array", new Long[] {10_000_000_000L}, List.of(10_000_000_000L)),
        Arguments.of("decimal_array", new Double[] {0.8, 1.2}, List.of(0.8, 1.2)),
        Arguments.of("bool_array", new Boolean[] {true, false}, List.of(true, false)),
        Arguments.of("date_array", new LocalDate[] {date}, List.of(epochDay)),
        Arguments.of("datetime_array", new LocalDateTime[] {dateTime}, List.of(millis)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("writtenValuesAndStoredValues")
  void givenValue_whenWriting_thenValueIsStoredInParquetEncoding(
      String columnName, Object rawValue, List<Object> expected) throws IOException {
    Path file = write(List.of(columnName), List.of(Row.row(columnName, rawValue)));

    assertEquals(expected, storedValues(readRows(file).getFirst(), columnName));
  }

  @Test
  void givenArrayWithNullItems_whenWriting_thenNullItemsAreLeftOut() throws IOException {
    Path file =
        write(List.of("int_array"), List.of(Row.row("int_array", new Integer[] {1, null, 3})));

    Group group = readRows(file).getFirst();
    assertEquals(2, group.getFieldRepetitionCount("int_array"));
    assertEquals(1, group.getInteger("int_array", 0));
    assertEquals(3, group.getInteger("int_array", 1));
  }

  @Test
  void givenEmptyAllNullOrMissingArrays_whenWriting_thenFieldsHaveNoValues() throws IOException {
    Row row = Row.row("int_array", new Integer[] {}, "decimal_array", new Double[] {null, null});

    Path file = write(List.of("int_array", "decimal_array", "bool_array"), List.of(row));

    Group group = readRows(file).getFirst();
    assertEquals(0, group.getFieldRepetitionCount("int_array"));
    assertEquals(0, group.getFieldRepetitionCount("decimal_array"));
    assertEquals(0, group.getFieldRepetitionCount("bool_array"));
  }

  @Test
  void givenColumnNotInTable_whenWriting_thenThrows() {
    List<String> columnNames = List.of("not_in_table");
    List<Row> rows = List.of(Row.row("not_in_table", "brown"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> write(columnNames, rows));
    assertEquals(
        "Cannot write column 'not_in_table' to Parquet: table 'products' has no column with that name",
        exception.getMessage());
  }

  @Test
  void givenMissingValues_whenWriting_thenValuesAreNull() throws IOException {
    Path file = write(List.of("string", "int"), List.of(Row.row("string", "Bread")));

    Group group = readRows(file).getFirst();
    assertEquals(1, group.getFieldRepetitionCount("string"));
    assertEquals(0, group.getFieldRepetitionCount("int"));
  }

  private Path write(List<String> columnNames, List<Row> rows) throws IOException {
    return write(table, columnNames, rows);
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

  private static void assertPrimitive(
      MessageType schema,
      String columnName,
      PrimitiveTypeName primitiveType,
      LogicalTypeAnnotation logicalType) {
    Type column = schema.getType(columnName);
    assertEquals(Type.Repetition.OPTIONAL, column.getRepetition());
    assertEquals(primitiveType, column.asPrimitiveType().getPrimitiveTypeName());
    assertEquals(logicalType, column.getLogicalTypeAnnotation());
  }

  private static List<Object> storedValues(Group group, String columnName) {
    PrimitiveTypeName primitiveType =
        group.getType().getType(columnName).asPrimitiveType().getPrimitiveTypeName();
    List<Object> values = new ArrayList<>();

    for (int i = 0; i < group.getFieldRepetitionCount(columnName); i++) {
      switch (primitiveType) {
        case INT32 -> values.add(group.getInteger(columnName, i));
        case INT64 -> values.add(group.getLong(columnName, i));
        case DOUBLE -> values.add(group.getDouble(columnName, i));
        case BOOLEAN -> values.add(group.getBoolean(columnName, i));
        case BINARY -> values.add(group.getString(columnName, i));
        default -> fail("No getter for " + primitiveType);
      }
    }

    return values;
  }

  private static MessageType fileSchema(Path file) throws IOException {
    try (ParquetFileReader reader = openReader(file)) {
      return reader.getFooter().getFileMetaData().getSchema();
    }
  }

  private static List<Group> readRows(Path file) throws IOException {
    try (ParquetFileReader reader = openReader(file)) {
      MessageType schema = reader.getFooter().getFileMetaData().getSchema();
      List<Group> rows = new ArrayList<>();
      PageReadStore rowGroup;
      while ((rowGroup = reader.readNextRowGroup()) != null) {
        RecordReader<Group> records =
            new ColumnIOFactory()
                .getColumnIO(schema)
                .getRecordReader(rowGroup, new GroupRecordConverter(schema));
        for (long i = 0; i < rowGroup.getRowCount(); i++) {
          rows.add(records.read());
        }
      }
      return rows;
    }
  }

  private static ParquetFileReader openReader(Path file) throws IOException {
    ParquetReadOptions options =
        ParquetReadOptions.builder(new PlainParquetConfiguration()).build();
    return ParquetFileReader.open(new LocalInputFile(file), options);
  }
}
