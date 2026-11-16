package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  class ReadTable {

    @Test
    void givenParquetFile_whenReadingTable_thenReturnRowsInOrder() throws IOException {
      TableStoreForParquet store = storeWithProductFile();

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3)),
          readAll(store, "Product"));
    }

    @Test
    void givenParquetFile_whenReadingTable_thenRowsContainAllColumns() throws IOException {
      TableStoreForParquet store = storeWithProductFile();

      Row row = store.readTable("Product").iterator().next();

      assertEquals(Set.of("name", "price"), row.getColumnNames());
    }

    @Test
    void givenNullValueInFile_whenReadingTable_thenValueIsNull() throws IOException {
      Path file = tmp.resolve("product.parquet");
      writeProductFile(file, product("Bread", null));
      TableStoreForParquet store = new TableStoreForParquet(Map.of("Product", file));

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", null)), readAll(store, "Product"));
    }

    @Test
    void givenUnmappedTable_whenReadingTable_thenThrow() {
      TableStoreForParquet store = new TableStoreForParquet(Map.of());

      assertThrows(MolgenisException.class, () -> readAll(store, "Unknown"));
    }

    @Test
    void givenMappedTableWithoutFile_whenReadingTable_thenThrow() {
      Path missing = tmp.resolve("missing.parquet");
      TableStoreForParquet store = new TableStoreForParquet(Map.of("Product", missing));

      assertThrows(MolgenisException.class, () -> readAll(store, "Product"));
    }
  }

  @Nested
  class WriteTable {

    @Test
    void givenMappedTable_whenWritingTable_thenReadBackRows() {
      TableStoreForParquet store = emptyProductStore();

      List<Row> rows =
          List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3));

      store.writeTable("Product", List.of("name", "price"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }

    @Test
    void givenMappedTable_whenWritingTable_thenFileIsCreatedAtMappedPath() {
      Path file = tmp.resolve("product.parquet");
      TableStoreForParquet store = new TableStoreForParquet(Map.of("Product", file));

      store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));

      assertTrue(Files.exists(file));
    }

    @Test
    void givenUnmappedTable_whenWritingTable_thenThrow() {
      TableStoreForParquet store = new TableStoreForParquet(Map.of());
      List<String> columns = List.of("name");
      List<Row> rows = List.of(Row.row("name", "Bread"));

      assertThrows(MolgenisException.class, () -> store.writeTable("Unknown", columns, rows));
    }

    @Test
    void givenRowsMissingColumn_whenWritingTable_thenFillWithNull() {
      TableStoreForParquet store = emptyProductStore();

      store.writeTable("Product", List.of("name", "price"), List.of(Row.row("name", "Bread")));

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", null)), readAll(store, "Product"));
    }

    @Test
    void givenRowsWithExtraColumns_whenWritingTable_thenDropExtraColumns() {
      TableStoreForParquet store = emptyProductStore();

      store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread", "price", 1)));

      CompareTools.assertEquals(List.of(Row.row("name", "Bread")), readAll(store, "Product"));
    }

    @Test
    void givenNoRows_whenWritingTable_thenTableExistsAndIsEmpty() {
      TableStoreForParquet store = emptyProductStore();

      store.writeTable("Product", List.of("name", "price"), List.of());

      assertTrue(store.containsTable("Product"));
      assertTrue(readAll(store, "Product").isEmpty());
    }

    @Test
    void givenExistingFile_whenWritingTable_thenReplaceRows() throws IOException {
      TableStoreForParquet store = storeWithProductFile();

      List<Row> rows = List.of(Row.row("name", "Milk"));

      store.writeTable("Product", List.of("name"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }

    @Test
    void givenColumnNameWithSpace_whenWritingTable_thenReadBackRows() {
      TableStoreForParquet store = emptyProductStore();
      List<Row> rows = List.of(Row.row("product name", "Bread"));

      store.writeTable("Product", List.of("product name"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }

    @Test
    void givenColumnNamesThatEncodeToSameName_whenWritingTable_thenReadBackRows() {
      TableStoreForParquet store = emptyProductStore();
      List<Row> rows = List.of(Row.row("product name", "Bread", "product_name", "Butter"));

      store.writeTable("Product", List.of("product name", "product_name"), rows);

      CompareTools.assertEquals(rows, readAll(store, "Product"));
    }
  }

  @Nested
  class WriteTableStreaming {

    @Test
    void givenRowProducer_whenWritingTableStreaming_thenReadBackRows() {
      TableStoreForParquet store = emptyProductStore();

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
    void givenTable_whenProcessingTable_thenProcessorReceivesAllRowsAndStore() throws IOException {
      TableStoreForParquet store = storeWithProductFile();
      List<Row> processed = new ArrayList<>();
      RowProcessor processor = (iterator, source) -> iterator.forEachRemaining(processed::add);

      store.processTable("Product", processor);

      CompareTools.assertEquals(
          List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3)),
          processed);
    }

    @Test
    void givenUnmappedTable_whenProcessingTable_thenThrow() {
      TableStoreForParquet store = new TableStoreForParquet(Map.of());
      RowProcessor processor = (iterator, source) -> iterator.forEachRemaining(row -> {});

      assertThrows(MolgenisException.class, () -> store.processTable("Unknown", processor));
    }
  }

  @Nested
  class ContainsTable {

    @Test
    void givenMappedTableWithFile_thenContainsTable() throws IOException {
      TableStoreForParquet store = storeWithProductFile();

      assertTrue(store.containsTable("Product"));
    }

    @Test
    void givenUnmappedTable_thenDoesNotContainTable() {
      TableStoreForParquet store = new TableStoreForParquet(Map.of());

      assertFalse(store.containsTable("Unknown"));
    }
  }

  @Nested
  class GetTableNames {

    @Test
    void givenMultipleTables_whenGettingTableNames_thenReturnNamesWithoutExtension()
        throws IOException {
      Path product = tmp.resolve("product.parquet");
      Path order = tmp.resolve("order.parquet");
      writeProductFile(product, product("Bread", 1));
      writeProductFile(order, product("Butter", 3));
      TableStoreForParquet store =
          new TableStoreForParquet(Map.of("Product", product, "Order", order));

      assertEquals(Set.of("Product", "Order"), Set.copyOf(store.getTableNames()));
    }

    @Test
    void givenMappedTableWithoutFile_whenGettingTableNames_thenLeaveItOut() throws IOException {
      Path product = tmp.resolve("product.parquet");
      writeProductFile(product, product("Bread", 1));
      TableStoreForParquet store =
          new TableStoreForParquet(
              Map.of("Product", product, "Order", tmp.resolve("order.parquet")));

      assertEquals(Set.of("Product"), Set.copyOf(store.getTableNames()));
    }

    @Test
    void givenNoTables_whenGettingTableNames_thenReturnEmpty() {
      TableStoreForParquet store = new TableStoreForParquet(Map.of());

      assertTrue(store.getTableNames().isEmpty());
    }
  }

  private TableStoreForParquet storeWithProductFile() throws IOException {
    Path file = tmp.resolve("product.parquet");
    writeProductFile(file, product("Bread", 1), product("Butter", 3));
    return new TableStoreForParquet(Map.of("Product", file));
  }

  private TableStoreForParquet emptyProductStore() {
    return new TableStoreForParquet(Map.of("Product", tmp.resolve("product.parquet")));
  }

  private static GenericRecord product(String name, Integer price) {
    GenericRecord record = new GenericData.Record(PRODUCT_SCHEMA);
    record.put("name", name);
    record.put("price", price);
    return record;
  }

  private static void writeProductFile(Path file, GenericRecord... records) throws IOException {
    try (ParquetWriter<GenericRecord> writer =
        AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(file))
            .withConf(new PlainParquetConfiguration())
            .withSchema(PRODUCT_SCHEMA)
            .withDataModel(GenericData.get())
            .build()) {
      for (GenericRecord record : records) {
        writer.write(record);
      }
    }
  }

  private static List<Row> readAll(TableStore store, String name) {
    List<Row> rows = new ArrayList<>();
    store.readTable(name).forEach(rows::add);
    return rows;
  }
}
