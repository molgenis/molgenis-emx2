package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

class SubjectColumnRemoverTest {

  private InMemoryTableStore tableStore;

  @BeforeEach
  void setup() {
    tableStore = new InMemoryTableStore();
  }

  private void store(String tableName, Row... rows) {
    Set<String> columnNames = new LinkedHashSet<>();
    for (Row row : rows) {
      columnNames.addAll(row.getColumnNames());
    }
    tableStore.writeTable(tableName, List.copyOf(columnNames), List.of(rows));
  }

  @Test
  void shouldRemoveAllSubjectColumnsFromRows() {
    store(
        "Collections",
        new Row("id", "col-1", "_subject_", "urn:col:1", "_subject_publisher", "urn:org:1"));

    SubjectColumnRemover.remove(tableStore);

    Row result = tableStore.readTable("Collections").iterator().next();
    CompareTools.assertEquals(new Row("id", "col-1"), result);
  }

  @Test
  void shouldRemoveSubjectColumnsFromEveryRowInTable() {
    store(
        "Collections",
        new Row("id", "col-1", "_subject_", "urn:col:1"),
        new Row("id", "col-2", "_subject_", "urn:col:2"));

    SubjectColumnRemover.remove(tableStore);

    Iterator<Row> rows = tableStore.readTable("Collections").iterator();
    CompareTools.assertEquals(new Row("id", "col-1"), rows.next());
    CompareTools.assertEquals(new Row("id", "col-2"), rows.next());
    assertFalse(rows.hasNext());
  }

  @Test
  void shouldProcessEveryTableInTheStore() {
    store("Collections", new Row("id", "col-1", "_subject_", "urn:col:1"));
    store("Catalogues", new Row("id", "cat-1", "_subject_", "urn:cat:1"));

    SubjectColumnRemover.remove(tableStore);

    CompareTools.assertEquals(
        new Row("id", "col-1"), tableStore.readTable("Collections").iterator().next());
    CompareTools.assertEquals(
        new Row("id", "cat-1"), tableStore.readTable("Catalogues").iterator().next());
  }

  @Test
  void shouldLeaveNonSubjectColumnsUntouched() {
    store("Collections", new Row("id", "col-1", "name", "foo"));

    SubjectColumnRemover.remove(tableStore);

    Row result = tableStore.readTable("Collections").iterator().next();
    CompareTools.assertEquals(new Row("id", "col-1", "name", "foo"), result);
  }

  @Test
  void shouldDoNothingForAnEmptyTable() {
    tableStore.writeTable("Collections", List.of("id"), List.of());

    assertDoesNotThrow(() -> SubjectColumnRemover.remove(tableStore));
  }

  @Test
  void shouldDoNothingWhenNoTablesExist() {
    assertDoesNotThrow(() -> SubjectColumnRemover.remove(tableStore));
  }
}
