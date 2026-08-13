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
        new TableMetadata("Organisations")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Contacts")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

    schema.create(
        new TableMetadata("Collections")
            .add(
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("publisher").setType(ColumnType.REF).setRefTable("Organisations"),
                Column.column("creator").setType(ColumnType.REF_ARRAY).setRefTable("Organisations"),
                Column.column("contactPoint").setType(ColumnType.REF).setRefTable("Contacts")));

    tableStore = new InMemoryTableStore();
  }

  @Test
  void shouldFillMissingReferenceUsingSubjectIri() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1", "_subject_publisher", "urn:org:1"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    assertEquals("org-1", collection().getString("publisher"));
  }

  @Test
  void shouldIgnoreNullValueRelations() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1", "_subject_publisher", null));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    assertNull(collection().getString("publisher"));
  }

  @Test
  void shouldFillMissingArrayReferenceUsingPipeSeparatedSubjectIris() {
    store(
        "Organisations",
        new Row("_subject_", "urn:org:1", "id", "org-1"),
        new Row("_subject_", "urn:org:2", "id", "org-2"));
    store(
        "Collections",
        new Row("id", "col-1", "_subject_creator", new Object[] {"urn:org:1", "urn:org:2"}));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    assertEquals("org-1,org-2", collection().getString("creator"));
  }

  @Test
  void shouldNotOverwriteAnAlreadyResolvedReference() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store(
        "Collections",
        new Row(
            "id", "col-1",
            "publisher", "manually-set-id",
            "_subject_publisher", "urn:org:1"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    assertEquals("manually-set-id", collection().getString("publisher"));
  }

  @Test
  void shouldLeaveReferenceEmptyWhenSubjectColumnIsAbsent() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    assertNull(collection().getString("publisher"));
  }

  @Test
  void shouldLeaveReferenceEmptyWhenNoMatchingTargetRowIsFound() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1", "_subject_publisher", "urn:org:unknown"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> resolver.process(tableStore));
    assertEquals(
        "Referencing non-existing row for table: Organisations, for subject: urn:org:unknown",
        exception.getMessage());
    assertNull(collection().getString("publisher"));
  }

  @Test
  void shouldResolveMultipleReferenceColumnsOnTheSameRowIndependently() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Contacts", new Row("_subject_", "urn:contact:1", "id", "contact-1"));
    store(
        "Collections",
        new Row(
            "id", "col-1",
            "_subject_publisher", "urn:org:1",
            "_subject_contactPoint", "urn:contact:1"));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    Row result = collection();
    assertEquals("org-1", result.getString("publisher"));
    assertEquals("contact-1", result.getString("contactPoint"));
  }

  @Test
  void shouldHandleNullColumnValueForInput() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Contacts", new Row("_subject_", "urn:contact:1", "id", "contact-1"));
    store(
        "Collections",
        new Row(
            "id", "col-1",
            "_subject_publisher", "urn:org:1",
            "publisher", null,
            "_subject_contactPoint", "urn:contact:1",
            "contactPoint", null));

    ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
    resolver.process(tableStore);

    Row result = collection();
    assertEquals("org-1", result.getString("publisher"));
    assertEquals("contact-1", result.getString("contactPoint"));
  }

  @Nested
  class CompositeKeyTest {

    @BeforeEach
    void setup() {
      Database database = TestDatabaseFactory.getTestDatabase();
      schema = database.dropCreateSchema(CompositeKeyTest.class.getSimpleName()).getMetadata();

      schema.create(
          new TableMetadata("Organisations")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(Column.column("name").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Contacts")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(Column.column("name").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Collections")
              .add(
                  Column.column("id").setType(ColumnType.STRING).setPkey(),
                  Column.column("publisher").setType(ColumnType.REF).setRefTable("Organisations"),
                  Column.column("creator")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Organisations")));
    }

    @Test
    void shouldFillMissingReferenceUsingSubjectIri() {
      store(
          "Organisations",
          new Row("_subject_", "urn:org:1", "id", "org-1", "name", "organisation-1"));
      store("Collections", new Row("id", "col-1", "_subject_publisher", "urn:org:1"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("org-1", collection().getString("publisher.id"));
      assertEquals("organisation-1", collection().getString("publisher.name"));
    }

    @Test
    void shouldFillMissingReferenceUsingSubjectIriForArrays() {
      store(
          "Organisations",
          new Row("_subject_", "urn:org:1", "id", "org-1", "name", "organisation-1"),
          new Row("_subject_", "urn:org:2", "id", "org-2", "name", "organisation-2"));
      store(
          "Collections",
          new Row("id", "col-1", "_subject_creator", new Object[] {"urn:org:1", "urn:org:2"}));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertArrayEquals(new String[] {"org-1", "org-2"}, collection().getStringArray("creator.id"));
      assertArrayEquals(
          new String[] {"organisation-1", "organisation-2"},
          collection().getStringArray("creator.name"));
    }
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
            "supplier",
            "supplier-1");
    CompareTools.assertEquals(order, expectedOrder);
    assertFalse(iterator.hasNext());
  }

  @Nested
  class BackReferenceTest {

    private SchemaMetadata schema;

    @BeforeEach
    void setup() {
      Database database = TestDatabaseFactory.getTestDatabase();
      schema = database.dropCreateSchema(BackReferenceTest.class.getSimpleName()).getMetadata();

      TableMetadata collections =
          schema.create(
              new TableMetadata("Collections")
                  .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Organisations")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(
                  Column.column("collection")
                      .setType(ColumnType.REF)
                      .setRefTable("Collections")
                      .setPkey()));

      schema.create(
          new TableMetadata("Contacts")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey())
              .add(
                  Column.column("collection")
                      .setType(ColumnType.REF)
                      .setRefTable("Collections")
                      .setPkey()));

      collections.add(
          Column.column("organisation").setType(ColumnType.REF).setRefTable("Organisations"),
          Column.column("contact").setType(ColumnType.REF_ARRAY).setRefTable("Contacts"));
    }

    @Test
    void shouldBackReferenceSingle() {
      // This row should receive a reference to the collection
      store(
          "Organisations",
          new Row("_subject_", "urn:org:1", "id", "org-1", "_subject_collection", "urn:col:1"));

      // This row should receive the composite key of the organisation, including its own id
      store(
          "Collections",
          new Row("_subject_", "urn:col:1", "id", "col-1", "_subject_organisation", "urn:org:1"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertEquals("col-1", organisation().getString("collection"));
      assertEquals("org-1", collection().getString("organisation.id"));
      assertEquals("col-1", collection().getString("organisation.collection"));
    }

    @Test
    void shouldBackReferenceArray() {
      // This row should receive a reference to the contact
      store(
          "Collections",
          new Row(
              "_subject_",
              "urn:col:1",
              "id",
              "col-1",
              "_subject_contact",
              new String[] {"urn:con:1"}));

      // This row should receive the composite key of the organisation, including its own id
      store(
          "Contacts",
          new Row("_subject_", "urn:con:1", "id", "con-1", "_subject_collection", "urn:col:1"));

      ResolveMissingPkPostProcessor resolver = new ResolveMissingPkPostProcessor(schema);
      resolver.process(tableStore);

      assertArrayEquals(new String[] {"con-1"}, collection().getStringArray("contact.id"));
      assertArrayEquals(new String[] {"col-1"}, collection().getStringArray("contact.collection"));

      assertEquals("col-1", contact().getString("collection"));
    }
  }

  /** Writes rows for a table, deriving the column header from the union of all row keys. */
  private void store(String tableName, Row... rows) {
    tableStore.writeTable(
        tableName,
        Arrays.stream(rows).flatMap(row -> row.getColumnNames().stream()).distinct().toList(),
        List.of(rows));
  }

  private Row collection() {
    return tableStore.readTable("Collections").iterator().next();
  }

  private Row contact() {
    return tableStore.readTable("Contacts").iterator().next();
  }

  private Row organisation() {
    return tableStore.readTable("Organisations").iterator().next();
  }
}
