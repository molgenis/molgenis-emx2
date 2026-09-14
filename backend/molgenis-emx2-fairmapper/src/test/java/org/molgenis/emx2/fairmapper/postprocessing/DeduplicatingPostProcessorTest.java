package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class DeduplicatingPostProcessorTest {

  private static final String SCHEMA_NAME = DeduplicatingPostProcessorTest.class.getSimpleName();

  private InMemoryTableStore tableStore;
  private SchemaMetadata schema;

  @BeforeEach
  void setup() {
    schema = new SchemaMetadata(SCHEMA_NAME);

    schema.create(
        new TableMetadata("Contacts")
            .add(
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("name").setType(ColumnType.STRING),
                Column.column("email").setType(ColumnType.EMAIL),
                Column.column("tags").setType(ColumnType.STRING_ARRAY)));

    schema.create(
        new TableMetadata("Organisations")
            .add(
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("name").setType(ColumnType.STRING)));

    tableStore = new InMemoryTableStore();
  }

  @Test
  void shouldMergeDuplicateRowsIntoOne() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo", "email", null),
        new Row("id", "contact-1", "name", null, "email", "foo@tue.nl"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(new Row("id", "contact-1", "name", "foo", "email", "foo@tue.nl")), contacts());
  }

  @Test
  void shouldMergeIdenticalRows() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-1", "name", "foo"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(List.of(new Row("id", "contact-1", "name", "foo")), contacts());
  }

  @Test
  void shouldMergeMoreThanTwoDuplicates() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-1", "email", "foo@tue.nl"),
        new Row("id", "contact-1", "tags", new String[] {"lead"}));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row(
                "id",
                "contact-1",
                "name",
                "foo",
                "email",
                "foo@tue.nl",
                "tags",
                new String[] {"lead"})),
        contacts());
  }

  @Test
  void shouldKeepRowsWithDifferentPrimaryKeyValues() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-2", "name", "foo"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row("id", "contact-1", "name", "foo"), new Row("id", "contact-2", "name", "foo")),
        contacts());
  }

  @Test
  void shouldKeepRowsThatConflictOnAValue() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-1", "name", "bar"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row("id", "contact-1", "name", "foo"), new Row("id", "contact-1", "name", "bar")),
        contacts());
  }

  @Test
  void shouldOnlyMergeCompatibleRowsWithinTheSamePrimaryKey() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-1", "email", "foo@tue.nl"),
        new Row("id", "contact-1", "name", "bar"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    // the second row is compatible with the first and merges into it, the third conflicts on
    // name and therefore survives on its own
    CompareTools.assertEquals(
        List.of(
            new Row("id", "contact-1", "name", "foo", "email", "foo@tue.nl"),
            new Row("id", "contact-1", "name", "bar", "email", null)),
        contacts());
  }

  @Test
  void shouldTreatEmptyValueAsAbsent() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-1", "name", ""));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(List.of(new Row("id", "contact-1", "name", "foo")), contacts());
  }

  @Test
  void shouldIgnoreSubjectColumnsWhenComparing() {
    store(
        "Contacts",
        new Row("_subject_", "urn:contact:a", "id", "contact-1", "name", "foo"),
        new Row("_subject_", "urn:contact:b", "id", "contact-1", "email", "foo@tue.nl"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row(
                "_subject_",
                "urn:contact:a",
                "id",
                "contact-1",
                "name",
                "foo",
                "email",
                "foo@tue.nl")),
        contacts());
  }

  @Test
  void shouldNotMergeRowsThatAreMissingAPrimaryKeyValue() {
    store(
        "Contacts",
        new Row("id", null, "name", "foo"),
        new Row("id", null, "name", "foo"),
        new Row("id", "contact-1", "name", "bar"),
        new Row("id", "contact-1", "name", "bar"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row("id", null, "name", "foo"),
            new Row("id", null, "name", "foo"),
            new Row("id", "contact-1", "name", "bar")),
        contacts());
  }

  @Test
  void shouldLeaveATableWithoutDuplicatesUntouched() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-2", "name", "bar"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row("id", "contact-1", "name", "foo"), new Row("id", "contact-2", "name", "bar")),
        contacts());
  }

  @Test
  void shouldProcessEveryTableInTheStore() {
    store(
        "Contacts",
        new Row("id", "contact-1", "name", "foo"),
        new Row("id", "contact-1", "email", "foo@tue.nl"));
    store(
        "Organisations",
        new Row("id", "org-1"),
        new Row("id", "org-1", "name", "Organisation One"));

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(new Row("id", "contact-1", "name", "foo", "email", "foo@tue.nl")), contacts());
    CompareTools.assertEquals(
        List.of(new Row("id", "org-1", "name", "Organisation One")), organisations());
  }

  @Test
  void shouldHandleTableWithoutRows() {
    store("Contacts");

    new DeduplicatingPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(List.of(), contacts());
  }

  @Nested
  class ArrayValueTest {

    @Test
    void shouldMergeRowsWithEqualArrayValues() {
      store(
          "Contacts",
          new Row("id", "contact-1", "tags", new String[] {"lead", "author"}),
          new Row("id", "contact-1", "tags", new String[] {"lead", "author"}));

      new DeduplicatingPostProcessor(schema).process(tableStore);

      CompareTools.assertEquals(
          List.of(new Row("id", "contact-1", "tags", new String[] {"lead", "author"})), contacts());
    }

    @Test
    void shouldKeepRowsWithDifferentArrayValues() {
      store(
          "Contacts",
          new Row("id", "contact-1", "tags", new String[] {"lead"}),
          new Row("id", "contact-1", "tags", new String[] {"author"}));

      new DeduplicatingPostProcessor(schema).process(tableStore);

      // TODO: Do we want to merge the arrays?
      CompareTools.assertEquals(
          List.of(
              new Row("id", "contact-1", "tags", new String[] {"lead"}),
              new Row("id", "contact-1", "tags", new String[] {"author"})),
          contacts());
    }

    @Test
    void shouldMergeWhenTheArrayIsAbsentInOneRow() {
      store(
          "Contacts",
          new Row("id", "contact-1", "tags", null),
          new Row("id", "contact-1", "tags", new String[] {"lead"}));

      new DeduplicatingPostProcessor(schema).process(tableStore);

      CompareTools.assertEquals(
          List.of(new Row("id", "contact-1", "tags", new String[] {"lead"})), contacts());
    }
  }

  @Nested
  class CompositeKeyTest {

    @BeforeEach
    void setup() {
      Database database = TestDatabaseFactory.getTestDatabase();
      schema = database.dropCreateSchema(CompositeKeyTest.class.getSimpleName()).getMetadata();

      schema.create(
          new TableMetadata("Resources")
              .add(Column.column("id").setType(ColumnType.STRING).setPkey()));

      schema.create(
          new TableMetadata("Contacts")
              .add(
                  Column.column("resource")
                      .setType(ColumnType.REF)
                      .setRefTable("Resources")
                      .setPkey(),
                  Column.column("name").setType(ColumnType.STRING).setPkey(),
                  Column.column("email").setType(ColumnType.EMAIL)));
    }

    @Test
    void shouldMergeRowsWithMatchingCompositeKey() {
      store(
          "Contacts",
          new Row("resource", "resource-1", "name", "foo"),
          new Row("resource", "resource-1", "name", "foo", "email", "foo@tue.nl"));

      new DeduplicatingPostProcessor(schema).process(tableStore);

      CompareTools.assertEquals(
          List.of(new Row("resource", "resource-1", "name", "foo", "email", "foo@tue.nl")),
          contacts());
    }

    @Test
    void shouldKeepRowsThatDifferInOnePartOfTheCompositeKey() {
      store(
          "Contacts",
          new Row("resource", "resource-1", "name", "foo"),
          new Row("resource", "resource-2", "name", "foo"),
          new Row("resource", "resource-1", "name", "bar"));

      new DeduplicatingPostProcessor(schema).process(tableStore);

      CompareTools.assertEquals(
          List.of(
              new Row("resource", "resource-1", "name", "foo"),
              new Row("resource", "resource-2", "name", "foo"),
              new Row("resource", "resource-1", "name", "bar")),
          contacts());
    }

    @Test
    void shouldIgnoreReferenceSubjectFields() {
      store(
          "Contacts",
          new Row("_subject_resource", "res-1", "resource", "resource-1", "name", "foo"),
          new Row(
              "_subject_resource", "res-2",
              "resource", "resource-1",
              "name", "foo",
              "email", "foo@tue.nl"));

      new DeduplicatingPostProcessor(schema).process(tableStore);

      CompareTools.assertEquals(
          List.of(
              new Row(
                  "_subject_resource",
                  "res-1",
                  "resource",
                  "resource-1",
                  "name",
                  "foo",
                  "email",
                  "foo@tue.nl")),
          contacts());
    }
  }

  /** Writes rows for a table, deriving the column header from the union of all row keys. */
  private void store(String tableName, Row... rows) {
    Set<String> columnNames = new LinkedHashSet<>();
    for (Row row : rows) {
      columnNames.addAll(row.getColumnNames());
    }
    tableStore.writeTable(tableName, List.copyOf(columnNames), List.of(rows));
  }

  private List<Row> contacts() {
    return toList(tableStore.readTable("Contacts"));
  }

  private List<Row> organisations() {
    return toList(tableStore.readTable("Organisations"));
  }

  private static List<Row> toList(Iterable<Row> rows) {
    return StreamSupport.stream(rows.spliterator(), false).toList();
  }
}
