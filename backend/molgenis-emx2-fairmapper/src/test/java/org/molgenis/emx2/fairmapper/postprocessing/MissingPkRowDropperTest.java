package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class MissingPkRowDropperTest {

  private static final String SCHEMA_NAME = MissingPkRowDropperTest.class.getSimpleName();

  private InMemoryTableStore tableStore;
  private SchemaMetadata schema;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME).getMetadata();

    schema.create(
        new TableMetadata("Products")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    // composite primary key: a row is only complete when both parts are present
    schema.create(
        new TableMetadata("Orders")
            .add(
                Column.column("orderId").setType(ColumnType.STRING).setPkey(),
                Column.column("productId").setType(ColumnType.STRING).setPkey()));

    tableStore = new InMemoryTableStore();
  }

  /** Writes rows for a table, deriving the column header from the union of all row keys. */
  private void store(String tableName, Row... rows) {
    Set<String> columnNames = new LinkedHashSet<>();
    for (Row row : rows) {
      columnNames.addAll(row.getColumnNames());
    }
    tableStore.writeTable(tableName, List.copyOf(columnNames), List.of(rows));
  }

  private List<Row> products() {
    return toList(tableStore.readTable("Products"));
  }

  private List<Row> orders() {
    return toList(tableStore.readTable("Orders"));
  }

  private static List<Row> toList(Iterable<Row> rows) {
    return StreamSupport.stream(rows.spliterator(), false).toList();
  }

  @Test
  void shouldKeepRowsThatHaveAllPkValues() {
    store("Products", new Row("id", "product-1"), new Row("id", "product-2"));

    new MissingPkRowDropper(schema, List.of("Products")).process(tableStore);

    CompareTools.assertEquals(
        List.of(new Row("id", "product-1"), new Row("id", "product-2")), products());
  }

  @Test
  void shouldDropRowMissingItsOnlyPkValue() {
    store("Products", new Row("id", "product-1"), new Row("id", null));

    new MissingPkRowDropper(schema, List.of("Products")).process(tableStore);

    CompareTools.assertEquals(List.of(new Row("id", "product-1")), products());
  }

  @Test
  void shouldDropRowWithRedactedPkValue() {
    store("Orders", new Row("orderId", "order-1"), new Row());

    new MissingPkRowDropper(schema, List.of("Orders")).process(tableStore);

    assertTrue(orders().isEmpty());
  }

  @Test
  void shouldDropRowMissingOneOfACompositePkValue() {
    store(
        "Orders",
        new Row("orderId", "order-1", "productId", "product-1"),
        new Row("orderId", "order-2", "productId", null));

    new MissingPkRowDropper(schema, List.of("Orders")).process(tableStore);

    CompareTools.assertEquals(
        List.of(new Row("orderId", "order-1", "productId", "product-1")), orders());
  }

  @Test
  void shouldOnlyProcessConfiguredTables() {
    store("Products", new Row("id", "product-1"), new Row("id", null));
    store("Orders", new Row("orderId", "order-1"));

    new MissingPkRowDropper(schema, List.of("Products")).process(tableStore);

    CompareTools.assertEquals(List.of(new Row("id", "product-1")), products());
    // Orders was not passed in, so its incomplete row must survive untouched
    CompareTools.assertEquals(List.of(new Row("orderId", "order-1")), orders());
  }

  @Test
  void shouldResultInEmptyTableWhenAllRowsAreDropped() {
    store("Products", new Row("id", null));

    new MissingPkRowDropper(schema, List.of("Products")).process(tableStore);

    assertTrue(products().isEmpty());
  }

  @Test
  void shouldProcessMultipleConfiguredTablesIndependently() {
    store("Products", new Row("id", "product-1"), new Row("id", null));
    store(
        "Orders",
        new Row("orderId", "order-1", "productId", "product-1"),
        new Row("orderId", "order-2", "productId", null));

    new MissingPkRowDropper(schema, List.of("Products", "Orders")).process(tableStore);

    CompareTools.assertEquals(List.of(new Row("id", "product-1")), products());
    CompareTools.assertEquals(
        List.of(new Row("orderId", "order-1", "productId", "product-1")), orders());
  }
}
