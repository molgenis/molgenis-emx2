package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Constants.TEXT_SEARCH_COLUMN_NAME;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.TableMetadata;

class SqlQueryBuilderHelpersTest {

  @Test
  void orderBy() {}

  @Test
  void getNonExistingColumn() {
    final TableMetadata table = new TableMetadata("testTable");
    assertThrows(
        MolgenisException.class, () -> SqlQueryBuilderHelpers.getColumnByName(table, "foo"));
  }

  @Test
  void getSearchColumn() {
    final TableMetadata table = new TableMetadata("testTable");
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, TEXT_SEARCH_COLUMN_NAME);
    assertEquals("testTable" + TEXT_SEARCH_COLUMN_NAME, column.getName());
  }

  @Test
  void getColumnByName() {
    final TableMetadata table = new TableMetadata("testTable");
    table.add(new Column("col1"));
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, "col1");
    assertEquals("col1", column.getName());
  }

  @Test
  void getRefColumnByName() {
    final TableMetadata table = Mockito.mock(TableMetadata.class);
    final Column col1 = Mockito.mock(Column.class);
    final Reference ref1 = Mockito.mock(Reference.class);
    when(ref1.getName()).thenReturn("ref1");
    when(col1.getName()).thenReturn("col1");
    when(col1.getReferences()).thenReturn(Collections.singletonList(ref1));
    when(table.getColumns()).thenReturn(Collections.singletonList(col1));
    when(table.getTableName()).thenReturn("testTable");
    when(ref1.getPrimitiveType()).thenReturn(ColumnType.REF);

    table.add(col1);
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, "ref1");
    assertEquals("ref1", column.getName());
  }

  @Test
  void getFileColumnByName() {
    final TableMetadata table = Mockito.mock(TableMetadata.class);
    when(table.getTableName()).thenReturn("testTable");
    final Column col1 = Mockito.mock(Column.class);
    when(col1.getName()).thenReturn("col1_contents");
    when(col1.isFile()).thenReturn(true);
    when(table.getColumns()).thenReturn(Collections.singletonList(col1));

    table.add(col1);
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, "col1_contents");
    assertEquals("col1_contents", column.getName());
  }
}
