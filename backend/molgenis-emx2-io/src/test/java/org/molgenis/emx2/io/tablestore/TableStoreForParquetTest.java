package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalOutputFile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

class TableStoreForParquetTest {

  private static final Schema PRODUCT_SCHEMA =
      SchemaBuilder.record("Product")
          .fields()
          .requiredString("name")
          .optionalInt("price")
          .endRecord();

  private @TempDir Path tmp;

  @Nested
  class Constructor {

    @Test
    void givenMissingDirectory_whenCreatingStore_thenThrow() {
      Path missing = tmp.resolve("missing");

      assertThrows(MolgenisException.class, () -> new TableStoreForParquet(missing));
    }

    @Test
    void givenFileInsteadOfDirectory_whenCreatingStore_thenThrow() throws IOException {
      Path file = Files.createFile(tmp.resolve("not-a-directory"));

      assertThrows(MolgenisException.class, () -> new TableStoreForParquet(file));
    }
  }

  @Nested
  class ReadTable {

    @Test
    void givenParquetFile_whenReadingTable_thenReturnRowsInOrder() throws IOException {
      writeProductFile("Product", product("Bread", 1), product("Butter", 3));

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3)),
          readAll(store(), "Product"));
    }

    @Test
    void givenParquetFile_whenReadingTable_thenRowsContainAllColumns() throws IOException {
      writeProductFile("Product", product("Bread", 1));

      Row row = store().readTable("Product").iterator().next();

      assertEquals(Set.of("name", "price"), row.getColumnNames());
    }

    @Test
    void givenNullValueInFile_whenReadingTable_thenValueIsNull() throws IOException {
      writeProductFile("Product", product("Bread", null));

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", null)), readAll(store(), "Product"));
    }

    @Test
    void givenTableWithoutFile_whenReadingTable_thenThrow() {
      TableStoreForParquet store = store();

      assertThrows(MolgenisException.class, () -> readAll(store, "Product"));
    }
  }

  @Nested
  class WriteTable {

    @Test
    void givenRows_whenWritingTable_thenReadBackRows() {
      TableStoreForParquet store = store();
      List<Row> rows =
          List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3));

      store.writeTable("Product", List.of("name", "price"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }

    @Test
    void givenTableName_whenWritingTable_thenFileIsNamedAfterTable() {
      store().writeTable("Order line", List.of("name"), List.of(Row.row("name", "Bread")));

      assertTrue(Files.exists(tmp.resolve("Order line.parquet")));
    }

    @Test
    void givenRowsMissingColumn_whenWritingTable_thenFillWithNull() {
      TableStoreForParquet store = store();

      store.writeTable("Product", List.of("name", "price"), List.of(Row.row("name", "Bread")));

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", null)), readAll(store, "Product"));
    }

    @Test
    void givenRowsWithExtraColumns_whenWritingTable_thenDropExtraColumns() {
      TableStoreForParquet store = store();

      store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread", "price", 1)));

      CompareTools.assertEquals(List.of(Row.row("name", "Bread")), readAll(store, "Product"));
    }

    @Test
    void givenNoRows_whenWritingTable_thenTableExistsAndIsEmpty() {
      TableStoreForParquet store = store();

      store.writeTable("Product", List.of("name", "price"), List.of());

      assertTrue(store.containsTable("Product"));
      assertTrue(readAll(store, "Product").isEmpty());
    }

    @Test
    void givenExistingFile_whenWritingTable_thenReplaceRows() throws IOException {
      writeProductFile("Product", product("Bread", 1), product("Butter", 3));
      TableStoreForParquet store = store();
      List<Row> rows = List.of(Row.row("name", "Milk"));

      store.writeTable("Product", List.of("name"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }

    @Test
    void givenColumnNameWithSpace_whenWritingTable_thenReadBackRows() {
      TableStoreForParquet store = store();
      List<Row> rows = List.of(Row.row("product name", "Bread"));

      store.writeTable("Product", List.of("product name"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }
  }

  @Nested
  class WriteTableStreaming {

    @Test
    void givenRowProducer_whenWritingTableStreaming_thenReadBackRows() {
      TableStoreForParquet store = store();

      store.writeTableStreaming(
          "Product",
          List.of("name"),
          consumer -> {
            consumer.accept(Row.row("name", "Bread"));
            consumer.accept(Row.row("name", "Butter"));
          });

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread"), Row.row("name", "Butter")), readAll(store, "Product"));
    }
  }

  @Nested
  class ProcessTable {

    @Test
    void givenTable_whenProcessingTable_thenProcessorReceivesAllRows() throws IOException {
      writeProductFile("Product", product("Bread", 1), product("Butter", 3));
      List<Row> processed = new ArrayList<>();
      RowProcessor processor = (iterator, source) -> iterator.forEachRemaining(processed::add);

      store().processTable("Product", processor);

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3)),
          processed);
    }

    @Test
    void givenTableWithoutFile_whenProcessingTable_thenThrow() {
      TableStoreForParquet store = store();
      RowProcessor processor = (iterator, source) -> iterator.forEachRemaining(row -> {});

      assertThrows(MolgenisException.class, () -> store.processTable("Product", processor));
    }
  }

  @Nested
  class ContainsTable {

    @Test
    void givenTableWithFile_thenContainsTable() throws IOException {
      writeProductFile("Product", product("Bread", 1));

      assertTrue(store().containsTable("Product"));
    }

    @Test
    void givenTableWithoutFile_thenDoesNotContainTable() {
      assertFalse(store().containsTable("Product"));
    }
  }

  @Nested
  class GetTableNames {

    @Test
    void givenParquetFiles_whenGettingTableNames_thenReturnNamesWithoutExtension()
        throws IOException {
      writeProductFile("Product", product("Bread", 1));
      writeProductFile("Order line", product("Butter", 3));

      assertEquals(Set.of("Product", "Order line"), Set.copyOf(store().getTableNames()));
    }

    @Test
    void givenEmptyDirectory_whenGettingTableNames_thenReturnEmpty() {
      assertTrue(store().getTableNames().isEmpty());
    }
  }

  private TableStoreForParquet store() {
    return new TableStoreForParquet(tmp);
  }

  private static GenericRecord product(String name, Integer price) {
    GenericRecord genericRecord = new GenericData.Record(PRODUCT_SCHEMA);
    genericRecord.put("name", name);
    genericRecord.put("price", price);
    return genericRecord;
  }

  private void writeProductFile(String tableName, GenericRecord... records) throws IOException {
    Path file = tmp.resolve(tableName + ".parquet");
    try (ParquetWriter<GenericRecord> writer =
        AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(file))
            .withConf(new PlainParquetConfiguration())
            .withSchema(PRODUCT_SCHEMA)
            .withDataModel(GenericData.get())
            .build()) {
      for (GenericRecord genericRecord : records) {
        writer.write(genericRecord);
      }
    }
  }

  private static List<Row> readAll(TableStore store, String name) {
    List<Row> rows = new ArrayList<>();
    store.readTable(name).forEach(rows::add);
    return rows;
  }
}
