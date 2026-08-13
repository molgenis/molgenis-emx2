package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ResolveMissingPkPostProcessorTest {

  private static final String SCHEMA_NAME = ResolveMissingPkPostProcessorTest.class.getSimpleName();

  private InMemoryTableStore tableStore;
  private SchemaMetadata schema;

  @BeforeEach
  void setup() {
    schema = new SchemaMetadata(SCHEMA_NAME);

    schema.create(
        new TableMetadata("Suppliers")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Customers")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Orders")
            .add(
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("supplier").setType(ColumnType.REF).setRefTable("Suppliers"),
                Column.column("alternateSuppliers")
                    .setType(ColumnType.REF_ARRAY)
                    .setRefTable("Suppliers"),
                Column.column("customer").setType(ColumnType.REF).setRefTable("Customers")));

    tableStore = new InMemoryTableStore();
  }

  @Nested
  class NoRefDefined {

    @Test
    void shouldLeaveReferenceEmptyWhenSubjectColumnIsAbsent() {
      store("Suppliers", new Row("_subject_", "urn:supplier:1", "id", "supplier-1"));
      store("Orders", new Row("id", "order-1"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertNull(order().getString("supplier"));
    }

    @Test
    void shouldLeaveReferenceEmptyWhenNoMatchingTargetRowIsFound() {
      store("Suppliers", new Row("_subject_", "urn:supplier:1", "id", "supplier-1"));
      store("Orders", new Row("id", "order-1", "_subject_supplier", "urn:supplier:unknown"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      MolgenisException exception =
          assertThrows(MolgenisException.class, () -> resolver.process(tableStore));
      assertEquals(
          "Referencing non-existing row for table: Suppliers, for subject: urn:supplier:unknown",
          exception.getMessage());
      assertNull(order().getString("supplier"));
    }
  }

  @Nested
  class SingleRefs {

    @Test
    void shouldFillMissingReferenceUsingSubjectIri() {
      store("Suppliers", new Row("_subject_", "urn:supplier:1", "id", "supplier-1"));
      store("Orders", new Row("id", "order-1", "_subject_supplier", "urn:supplier:1"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("supplier-1", order().getString("supplier"));
    }

    @Test
    void shouldFillNullReferenceValuesUsingSubjectIri() {
      store("Suppliers", new Row("_subject_", "urn:supplier:1", "id", "supplier-1"));
      store(
          "Orders",
          new Row("id", "order-1", "_subject_supplier", "urn:supplier:1", "supplier", null));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("supplier-1", order().getString("supplier"));
    }

    @Test
    void shouldIgnoreNullValueRelations() {
      store("Suppliers", new Row("_subject_", "urn:supplier:1", "id", "supplier-1"));
      store("Orders", new Row("id", "order-1", "_subject_supplier", null));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertNull(order().getString("supplier"));
    }
  }

  @Nested
  class ArrayRefs {

    @Test
    void shouldFillMissingArrayReferenceUsingWhereEmptyArray() {
      store(
          "Suppliers",
          new Row("_subject_", "urn:supplier:1", "id", "supplier-1"),
          new Row("_subject_", "urn:supplier:2", "id", "supplier-2"));
      store(
          "Orders",
          new Row(
              "id",
              "order-1",
              "_subject_alternateSuppliers",
              new Object[] {"urn:supplier:1", "urn:supplier:2"},
              "alternateSuppliers.id",
              new String[0]));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("supplier-1,supplier-2", order().getString("alternateSuppliers"));
    }

    @Test
    void shouldFillMissingArrayReferenceUsingWhereNotPresent() {
      store(
          "Suppliers",
          new Row("_subject_", "urn:supplier:1", "id", "supplier-1"),
          new Row("_subject_", "urn:supplier:2", "id", "supplier-2"));
      store(
          "Orders",
          new Row(
              "id",
              "order-1",
              "_subject_alternateSuppliers",
              new Object[] {"urn:supplier:1", "urn:supplier:2"}));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertArrayEquals(
          new String[] {"supplier-1", "supplier-2"}, order().getStringArray("alternateSuppliers"));
    }

    @Test
    void shouldNotOverwriteAnAlreadyResolvedReference() {
      store(
          "Suppliers",
          new Row("_subject_", "urn:supplier:1", "id", "supplier-1"),
          new Row("_subject_", "urn:supplier:2", "id", "supplier-2"));
      store(
          "Orders",
          new Row(
              "id",
              "order-1",
              "_subject_alternateSuppliers",
              new Object[] {"urn:supplier:1", "urn:supplier:2"},
              "alternateSuppliers",
              new String[] {"manually-set-1", "manually-set-2"}));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertArrayEquals(
          new String[] {"manually-set-1", "manually-set-2"},
          order().getStringArray("alternateSuppliers"));
    }
  }

  @Nested
  class CompositeKeyTest {

    @BeforeEach
    void setup() {
      Database database = TestDatabaseFactory.getTestDatabase();
      schema = database.dropCreateSchema(CompositeKeyTest.class.getSimpleName()).getMetadata();

      schema.create(
          new TableMetadata("Suppliers")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(Column.column("name").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Customers")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(Column.column("name").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Orders")
              .add(
                  Column.column("id").setType(ColumnType.STRING).setPkey(),
                  Column.column("supplier").setType(ColumnType.REF).setRefTable("Suppliers"),
                  Column.column("alternateSuppliers")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Suppliers")));
    }

    @Test
    void single_compositeKeyReference() {
      store(
          "Suppliers",
          new Row("_subject_", "urn:supplier:1", "id", "supplier-1", "name", "supplier 1"));
      store(
          "Orders",
          new Row("id", "order-1", "_subject_supplier", "urn:supplier:1", "supplier.name", null));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("supplier-1", order().getString("supplier.id"));
      assertEquals("supplier 1", order().getString("supplier.name"));
    }

    @Test
    void arrays_compositeKeyReference() {
      store(
          "Suppliers",
          new Row("_subject_", "urn:supplier:1", "id", "supplier-1", "name", "supplier 1"),
          new Row("_subject_", "urn:supplier:2", "id", "supplier-2", "name", "supplier 2"));
      store(
          "Orders",
          new Row(
              "id",
              "order-1",
              "_subject_alternateSuppliers",
              new Object[] {"urn:supplier:1", "urn:supplier:2"},
              "alternateSuppliers.name",
              new String[0]));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertArrayEquals(
          new String[] {"supplier-1", "supplier-2"},
          order().getStringArray("alternateSuppliers.id"));
      assertArrayEquals(
          new String[] {"supplier 1", "supplier 2"},
          order().getStringArray("alternateSuppliers.name"));
    }
  }

  @Test
  void shouldResolveMultipleReferenceColumnsOnTheSameRowIndependently() {
    store("Suppliers", new Row("_subject_", "urn:supplier:1", "id", "supplier-1"));
    store("Customers", new Row("_subject_", "urn:customer:1", "id", "customer-1"));
    store(
        "Orders",
        new Row(
            "id", "order-1",
            "_subject_supplier", "urn:supplier:1",
            "_subject_customer", "urn:customer:1"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    Row result = order();
    assertEquals("supplier-1", result.getString("supplier"));
    assertEquals("customer-1", result.getString("customer"));
  }

  @Test
  void givenResolving_whenTableUpdatedAfterHandledByResolver_thenResolveAgain() {
    schema = new SchemaMetadata(SCHEMA_NAME + "OrderDependency");
    tableStore = new InMemoryTableStore();

    // Orders is created (and therefore processed) before Products exists, so it stays first in
    // table processing order even after the "product" reference column
    TableMetadata orders =
        schema.create(
            new TableMetadata("Orders")
                .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Suppliers")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Products")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey())
            .add(
                Column.column("supplier")
                    .setType(ColumnType.REF)
                    .setRefTable("Suppliers")
                    .setPkey()));

    orders.add(Column.column("product").setType(ColumnType.REF).setRefTable("Products"));

    // Orders must be processed before Products resolves its own composite key, or this test doesn't
    // reproduce the bug
    assertEquals(
        List.of("Orders", "Suppliers", "Products"), schema.getTableNames().stream().toList());

    store("Suppliers", new Row("_subject_", "urn:s:1", "id", "supplier-1"));
    store(
        "Products",
        new Row("_subject_", "urn:p:1", "id", "product-1", "_subject_supplier", "urn:s:1"));
    store("Orders", new Row("id", "order-1", "_subject_product", "urn:p:1"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    Iterator<Row> iterator = tableStore.readTable("Products").iterator();
    Row product = iterator.next();
    Row expectedProduct =
        Row.row(
            "_subject_",
            "urn:p:1",
            "id",
            "product-1",
            "_subject_supplier",
            "urn:s:1",
            "supplier",
            "supplier-1");
    CompareTools.assertEquals(product, expectedProduct);
    assertFalse(iterator.hasNext());

    iterator = tableStore.readTable("Orders").iterator();
    Row order = iterator.next();
    Row expectedOrder =
        Row.row(
            "id",
            "order-1",
            "_subject_product",
            "urn:p:1",
            "product.id",
            "product-1",
            "product.supplier",
            "supplier-1");
    CompareTools.assertEquals(order, expectedOrder);
    assertFalse(iterator.hasNext());
  }

  @Test
  void givenResolving_whenTableUpdatedAfterHandledByResolver_thenResolveAgainForArrayReference() {
    schema = new SchemaMetadata(SCHEMA_NAME + "OrderDependencyArray");
    tableStore = new InMemoryTableStore();

    // Orders is created (and therefore processed) before Products exists, so it stays first in
    // table processing order even after the "products" reference column is retrofitted onto it
    // below, once Products is available to reference.
    TableMetadata orders =
        schema.create(
            new TableMetadata("Orders")
                .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Suppliers")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Products")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey())
            .add(
                Column.column("supplier")
                    .setType(ColumnType.REF)
                    .setRefTable("Suppliers")
                    .setPkey()));

    orders.add(Column.column("products").setType(ColumnType.REF_ARRAY).setRefTable("Products"));

    // Orders must be processed before Products resolves its own composite key, or this test
    // doesn't reproduce the bug.
    assertEquals(
        List.of("Orders", "Suppliers", "Products"), schema.getTableNames().stream().toList());

    store("Suppliers", new Row("_subject_", "urn:s:1", "id", "supplier-1"));
    store(
        "Products",
        new Row("_subject_", "urn:p:1", "id", "product-1", "_subject_supplier", "urn:s:1"));
    store(
        "Orders",
        new Row(
            "id",
            "order-1",
            "_subject_products",
            new String[] {"urn:p:1"},
            "products.supplier",
            new String[0]));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    // Products only resolves its own "supplier" reference during its own pass, which runs
    // after Orders has already been handled. Orders should be resolved again once that new
    // information becomes available, instead of keeping the incomplete composite key array it
    // read on its first pass.
    Row order = tableStore.readTable("Orders").iterator().next();
    assertArrayEquals(new String[] {"product-1"}, order.getStringArray("products.id"));
    assertArrayEquals(new String[] {"supplier-1"}, order.getStringArray("products.supplier"));
  }

  @Nested
  class BackReferenceTest {

    private SchemaMetadata schema;

    @BeforeEach
    void setup() {
      Database database = TestDatabaseFactory.getTestDatabase();
      schema = database.dropCreateSchema(BackReferenceTest.class.getSimpleName()).getMetadata();

      TableMetadata orders =
          schema.create(
              new TableMetadata("Orders")
                  .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Suppliers")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(Column.column("order").setType(ColumnType.REF).setRefTable("Orders").setPkey()));

      schema.create(
          new TableMetadata("Customers")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(Column.column("order").setType(ColumnType.REF).setRefTable("Orders").setPkey()));

      orders.add(
          Column.column("supplier").setType(ColumnType.REF).setRefTable("Suppliers"),
          Column.column("customers").setType(ColumnType.REF_ARRAY).setRefTable("Customers"));
    }

    @Test
    void shouldBackReferenceSingle() {
      // This row should receive a reference to the order
      store(
          "Suppliers",
          new Row(
              "_subject_", "urn:supplier:1", "id", "supplier-1", "_subject_order", "urn:order:1"));

      // This row should receive the composite key of the supplier, including its own id
      store(
          "Orders",
          new Row(
              "_subject_",
              "urn:order:1",
              "id",
              "order-1",
              "_subject_supplier",
              "urn:supplier:1",
              "supplier.id",
              null,
              "supplier.order",
              null));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("order-1", supplier().getString("order"));
      assertEquals("supplier-1", order().getString("supplier.id"));
      assertEquals("order-1", order().getString("supplier.order"));
    }

    @Test
    void shouldBackReferenceArray() {
      // This row should receive a reference to the customer
      store(
          "Orders",
          new Row(
              "_subject_",
              "urn:order:1",
              "id",
              "order-1",
              "_subject_customers",
              new String[] {"urn:customer:1"},
              "customer.id",
              new String[0],
              "customer.order",
              new String[0]));

      // This row should receive the composite key of the order, including its own id
      store(
          "Customers",
          new Row(
              "_subject_", "urn:customer:1", "id", "customer-1", "_subject_order", "urn:order:1"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertArrayEquals(new String[] {"customer-1"}, order().getStringArray("customers.id"));
      assertArrayEquals(new String[] {"order-1"}, order().getStringArray("customers.order"));

      assertEquals("order-1", customer().getString("order"));
    }
  }

  /** Writes rows for a table, deriving the column header from the union of all row keys. */
  private void store(String tableName, Row... rows) {
    tableStore.writeTable(
        tableName,
        Arrays.stream(rows).flatMap(row -> row.getColumnNames().stream()).distinct().toList(),
        List.of(rows));
  }

  private Row order() {
    return tableStore.readTable("Orders").iterator().next();
  }

  private Row customer() {
    return tableStore.readTable("Customers").iterator().next();
  }

  private Row supplier() {
    return tableStore.readTable("Suppliers").iterator().next();
  }
}
