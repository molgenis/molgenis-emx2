package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.datamodels.util.CompareTools.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ResolveRefbackPostProcessorTest {

  private static final String SCHEMA_NAME = ResolveRefbackPostProcessorTest.class.getSimpleName();
  private static final String PERSON_TABLE = "person";
  private static final String PET_TABLE = "pet";

  private ResolveRefbackPostProcessor resolver;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    SchemaMetadata schema = database.dropCreateSchema(SCHEMA_NAME).getMetadata();

    schema.create(new TableMetadata(PERSON_TABLE).add(column("name").setType(STRING).setPkey()));

    schema.create(
        new TableMetadata(PET_TABLE)
            .add(
                column("name").setType(STRING).setPkey(),
                column("owner").setType(REF).setRefTable(PERSON_TABLE)));

    schema
        .getTableMetadata(PERSON_TABLE)
        .add(column("pets").setType(REFBACK).setRefTable(PET_TABLE).setRefBack("owner"));

    resolver = new ResolveRefbackPostProcessor(schema);
  }

  @Test
  void shouldResolveSingleReferencedRow() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(
        tableStore,
        PERSON_TABLE,
        new Row("_subject_", "person1", "name", "Alice", "_subject_pets", "pet1"));
    writeRows(tableStore, PET_TABLE, new Row("_subject_", "pet1", "name", "Rex"));

    resolver.process(tableStore);

    assertTableStoreHasTableWithRows(
        tableStore,
        PET_TABLE,
        new Row("_subject_", "pet1", "name", "Rex", "_subject_owner", "person1"));
  }

  @Test
  void shouldResolveAllReferencedRowsInArray() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(
        tableStore,
        PERSON_TABLE,
        new Row("_subject_", "person1", "name", "Alice", "_subject_pets", "pet1,pet2"));
    writeRows(
        tableStore,
        PET_TABLE,
        new Row("_subject_", "pet1", "name", "Rex"),
        new Row("_subject_", "pet2", "name", "Fido"));

    resolver.process(tableStore);

    assertTableStoreHasTableWithRows(
        tableStore,
        PET_TABLE,
        new Row("_subject_", "pet1", "name", "Rex", "_subject_owner", "person1"),
        new Row("_subject_", "pet2", "name", "Fido", "_subject_owner", "person1"));
  }

  @Test
  void shouldResolveEachPersonRowIndependently() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(
        tableStore,
        PERSON_TABLE,
        new Row("_subject_", "person1", "name", "Alice", "_subject_pets", "pet1"),
        new Row("_subject_", "person2", "name", "Bob", "_subject_pets", "pet2"));
    writeRows(
        tableStore,
        PET_TABLE,
        new Row("_subject_", "pet1", "name", "Rex"),
        new Row("_subject_", "pet2", "name", "Fido"));

    resolver.process(tableStore);

    assertTableStoreHasTableWithRows(
        tableStore,
        PET_TABLE,
        new Row("_subject_", "pet1", "name", "Rex", "_subject_owner", "person1"),
        new Row("_subject_", "pet2", "name", "Fido", "_subject_owner", "person2"));
  }

  @Test
  void givenPersonRow_whenSubjectFieldForRefbackIsMissing_thenReferencedRowIsUnchanged() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(tableStore, PERSON_TABLE, new Row("_subject_", "person1", "name", "Alice"));
    writeRows(tableStore, PET_TABLE, new Row("_subject_", "pet1", "name", "Rex"));

    resolver.process(tableStore);

    assertTableStoreHasTableWithRows(
        tableStore, PET_TABLE, new Row("_subject_", "pet1", "name", "Rex"));
  }

  @Test
  void givenTableWithoutRefbackColumns_whenProcessed_thenRowsAreUnchanged() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(tableStore, PET_TABLE, new Row("_subject_", "pet1", "name", "Rex"));

    resolver.process(tableStore);

    assertTableStoreHasTableWithRows(
        tableStore, PET_TABLE, new Row("_subject_", "pet1", "name", "Rex"));
  }

  @Test
  void givenReferencedSubject_whenRowDoesNotExistInRefTable_thenThrows() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(
        tableStore,
        PERSON_TABLE,
        new Row("_subject_", "person1", "name", "Alice", "_subject_pets", "pet-missing"));
    writeRows(tableStore, PET_TABLE, new Row("_subject_", "pet1", "name", "Rex"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> resolver.process(tableStore));

    assertTrue(exception.getMessage().contains(PET_TABLE));
    assertTrue(exception.getMessage().contains("pet-missing"));
  }

  private void writeRows(InMemoryTableStore tableStore, String table, Row... rows) {
    tableStore.writeTable(table, new ArrayList<>(rows[0].getColumnNames()), Arrays.asList(rows));
  }

  private void assertTableStoreHasTableWithRows(TableStore tableStore, String table, Row... rows) {
    List<Row> actual =
        StreamSupport.stream(tableStore.readTable(table).spliterator(), false).toList();
    List<Row> expected = Arrays.stream(rows).toList();
    assertEquals(actual, expected);
  }
}
