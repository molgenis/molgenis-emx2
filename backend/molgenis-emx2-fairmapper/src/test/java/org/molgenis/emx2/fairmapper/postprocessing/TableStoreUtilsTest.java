package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

class TableStoreUtilsTest {

  private SchemaMetadata inheritanceSchema() {
    SchemaMetadata schema = new SchemaMetadata("TableStoreUtilsTest");
    schema.create(table("Person", column("name").setType(STRING).setPkey()));
    schema.create(table("Employee", column("salary").setType(STRING)).setInheritName("Person"));
    schema.create(table("Manager", column("directs").setType(STRING)).setInheritName("Employee"));
    return schema;
  }

  @Test
  void shouldReturnRowFromGivenTableWhenPresent() {
    SchemaMetadata schema = new SchemaMetadata("TableStoreUtilsTest");
    TableMetadata animal = schema.create(table("Animal", column("name").setType(STRING).setPkey()));

    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(tableStore, "Animal", new Row("_subject_", "subject1", "name", "Rex"));

    Row result = TableStoreUtils.getRowForSubject(tableStore, animal, "subject1");

    assertEquals("Rex", result.getString("name"));
  }

  @Test
  void shouldReturnRowFromDirectSubclassWhenBaseTableAbsentFromStore() {
    SchemaMetadata schema = inheritanceSchema();
    TableMetadata person = schema.getTableMetadata("Person");

    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(tableStore, "Employee", new Row("_subject_", "subject1", "salary", "5000"));

    Row result = TableStoreUtils.getRowForSubject(tableStore, person, "subject1");

    assertEquals("5000", result.getString("salary"));
  }

  @Test
  void shouldReturnRowFromNestedSubclassWhenIntermediateTableAbsentFromStore() {
    SchemaMetadata schema = inheritanceSchema();
    TableMetadata person = schema.getTableMetadata("Person");

    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(tableStore, "Manager", new Row("_subject_", "subject1", "directs", "team-a"));

    Row result = TableStoreUtils.getRowForSubject(tableStore, person, "subject1");

    assertEquals("team-a", result.getString("directs"));
  }

  @Test
  void shouldThrowWhenNoTableInInheritanceTreeIsPresentInStore() {
    SchemaMetadata schema = inheritanceSchema();
    TableMetadata person = schema.getTableMetadata("Person");

    InMemoryTableStore tableStore = new InMemoryTableStore();

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> TableStoreUtils.getRowForSubject(tableStore, person, "subject1"));

    assertTrue(exception.getMessage().contains("Person"));
    assertTrue(exception.getMessage().contains("subject1"));
  }

  @Test
  void shouldThrowWhenPresentTablesHaveNoMatchingSubject() {
    SchemaMetadata schema = inheritanceSchema();
    TableMetadata person = schema.getTableMetadata("Person");

    InMemoryTableStore tableStore = new InMemoryTableStore();
    writeRows(tableStore, "Person", new Row("_subject_", "other-subject", "name", "Alice"));
    writeRows(tableStore, "Employee", new Row("_subject_", "other-subject", "salary", "5000"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> TableStoreUtils.getRowForSubject(tableStore, person, "subject1"));

    assertTrue(exception.getMessage().contains("Person"));
    assertTrue(exception.getMessage().contains("subject1"));
  }

  private void writeRows(InMemoryTableStore tableStore, String table, Row... rows) {
    tableStore.writeTable(table, new ArrayList<>(rows[0].getColumnNames()), List.of(rows));
  }
}
