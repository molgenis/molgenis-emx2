package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

class TableStoreForParquetTest {

  private static final SchemaMetadata SCHEMA =
      new SchemaMetadata("shop").create(productTable("Product"), productTable("Order line"));

  private @TempDir Path tmp;

  @Nested
  class Constructor {

    @Test
    void givenMissingDirectory_whenCreatingStore_thenThrow() {
      Path missing = tmp.resolve("missing");

      assertThrows(MolgenisException.class, () -> new TableStoreForParquet(missing, SCHEMA));
    }

    @Test
    void givenFileInsteadOfDirectory_whenCreatingStore_thenThrow() throws IOException {
      Path file = Files.createFile(tmp.resolve("not-a-directory"));

      assertThrows(MolgenisException.class, () -> new TableStoreForParquet(file, SCHEMA));
    }
  }

  @Nested
  class ReadTable {

    @Test
    void givenTableWithoutFile_whenReadingTable_thenThrow() {
      TableStoreForParquet store = store();

      assertThrows(MolgenisException.class, () -> readAll(store));
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

      CompareTools.assertEquals(rows, readAll(store));
    }

    @Test
    void givenTableName_whenWritingTable_thenFileIsNamedAfterTable() {
      store().writeTable("Order line", List.of("name"), List.of(Row.row("name", "Bread")));

      assertTrue(Files.exists(tmp.resolve("Order line.parquet")));
    }

    @Test
    void givenNoRows_whenWritingTable_thenTableExistsAndIsEmpty() {
      TableStoreForParquet store = store();

      store.writeTable("Product", List.of("name", "price"), List.of());

      assertTrue(store.containsTable("Product"));
      assertTrue(readAll(store).isEmpty());
    }

    @Test
    void givenExistingFile_whenWritingTable_thenReplaceRows() {
      writeProductFile("Product", product("Bread", 1), product("Butter", 3));
      TableStoreForParquet store = store();
      List<Row> rows = List.of(Row.row("name", "Milk"));

      store.writeTable("Product", List.of("name"), rows);

      CompareTools.assertEquals(rows, readAll(store));
    }

    @Test
    void givenTableNotInSchema_whenWritingTable_thenThrow() {
      TableStoreForParquet store = store();
      List<String> columnNames = List.of("name");
      List<Row> rows = List.of(Row.row("name", "Bread"));

      assertThrows(MolgenisException.class, () -> store.writeTable("Unknown", columnNames, rows));
    }

    @Test
    void givenColumnNameWithSpace_whenWritingTable_thenReadBackRows() {
      TableStoreForParquet store = store();
      List<Row> rows = List.of(Row.row("product name", "Bread"));

      store.writeTable("Product", List.of("product name"), rows);

      CompareTools.assertEquals(rows, readAll(store));
    }
  }

  @Nested
  class ProcessTable {

    @Test
    void givenTable_whenProcessingTable_thenProcessorReceivesAllRows() {
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
    void givenTableWithFile_thenContainsTable() {
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
    void givenParquetFiles_whenGettingTableNames_thenReturnNamesWithoutExtension() {
      writeProductFile("Product", product("Bread", 1));
      writeProductFile("Order line", product("Butter", 3));

      assertEquals(Set.of("Product", "Order line"), Set.copyOf(store().getTableNames()));
    }

    @Test
    void givenEmptyDirectory_whenGettingTableNames_thenReturnEmpty() {
      assertTrue(store().getTableNames().isEmpty());
    }
  }

  private static TableMetadata productTable(String name) {
    return new TableMetadata(name)
        .add(
            column("name").setType(ColumnType.STRING),
            column("price").setType(ColumnType.INT),
            column("product name").setType(ColumnType.STRING));
  }

  private TableStoreForParquet store() {
    return new TableStoreForParquet(tmp, SCHEMA);
  }

  private static Row product(String name, Integer price) {
    return Row.row("name", name, "price", price);
  }

  private void writeProductFile(String tableName, Row... rows) {
    store().writeTable(tableName, List.of("name", "price"), List.of(rows));
  }

  private static List<Row> readAll(TableStore store) {
    List<Row> rows = new ArrayList<>();
    store.readTable("Product").forEach(rows::add);
    return rows;
  }
}
