package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class MissingPrimaryKeyResolverTest {

  private static final String SCHEMA_NAME = MissingPrimaryKeyResolverTest.class.getSimpleName();

  private InMemoryTableStore tableStore;
  private MissingReferencePrimaryKeyResolver resolver;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    SchemaMetadata schema = database.dropCreateSchema(SCHEMA_NAME).getMetadata();

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
    resolver = new MissingReferencePrimaryKeyResolver(schema);
  }

  /** Writes rows for a table, deriving the column header from the union of all row keys. */
  private void store(String tableName, Row... rows) {
    Set<String> columnNames = new LinkedHashSet<>();
    for (Row row : rows) {
      columnNames.addAll(row.getColumnNames());
    }
    tableStore.writeTable(tableName, List.copyOf(columnNames), List.of(rows));
  }

  private Row collection() {
    return tableStore.readTable("Collections").iterator().next();
  }

  @Test
  void shouldFillMissingReferenceUsingSubjectIri() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1", "_subject_publisher", "urn:org:1"));

    resolver.resolve(tableStore, "Collections");

    assertEquals("org-1", collection().getString("publisher"));
  }

  @Test
  void shouldFillMissingArrayReferenceUsingCommaSeparatedSubjectIris() {
    store(
        "Organisations",
        new Row("_subject_", "urn:org:1", "id", "org-1"),
        new Row("_subject_", "urn:org:2", "id", "org-2"));
    store("Collections", new Row("id", "col-1", "_subject_creator", "urn:org:1\u001Furn:org:2"));

    resolver.resolve(tableStore, "Collections");

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

    resolver.resolve(tableStore, "Collections");

    assertEquals("manually-set-id", collection().getString("publisher"));
  }

  @Test
  void shouldLeaveReferenceEmptyWhenSubjectColumnIsAbsent() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1"));

    resolver.resolve(tableStore, "Collections");

    assertNull(collection().getString("publisher"));
  }

  @Test
  void shouldLeaveReferenceEmptyWhenNoMatchingTargetRowIsFound() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1", "_subject_publisher", "urn:org:unknown"));

    ;

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> resolver.resolve(tableStore, "Collections"));
    assertEquals(
        "Referencing non-existing row for table: Organisations, for subject: urn:org:unknown",
        exception.getMessage());
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

    resolver.resolve(tableStore, "Collections");

    Row result = collection();
    assertEquals("org-1", result.getString("publisher"));
    assertEquals("contact-1", result.getString("contactPoint"));
  }

  @Test
  void shouldOnlyResolveTablesThatWerePassedIn() {
    store("Organisations", new Row("_subject_", "urn:org:1", "id", "org-1"));
    store("Collections", new Row("id", "col-1", "_subject_publisher", "urn:org:1"));

    resolver.resolve(tableStore);

    assertNull(collection().getString("publisher"));
  }
}
