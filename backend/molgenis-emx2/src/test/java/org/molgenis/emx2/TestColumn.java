package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TestColumn {
  @Test
  public void isSystemColumn() {
    assertAll(
        () -> assertFalse(new Column("test").isSystemColumn()),
        () -> assertTrue(new Column("mg_test").isSystemColumn()),
        () -> assertFalse(new Column("MG_test").isSystemColumn()));
  }

  @Test
  public void validColumnName() {
    assertAll(
        // valid: 1 or more legal characters
        () -> assertDoesNotThrow(() -> new Column("a")),
        // valid: a space
        () -> assertDoesNotThrow(() -> new Column("first name")),
        // valid: space & underscore but not next to each other
        () -> assertDoesNotThrow(() -> new Column("yet_another name")),
        // invalid: # should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("#first name")),
        // invalid: '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first_  name")),
        // invalid: ' _' not allowed
        () -> assertThrows(MolgenisException.class, () -> new Column("first   _name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first  _  name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first  __   name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("aa    ____      ")),
        // valid: max length (= 63 characters) -> psql limit (as of 2024-09-03):
        // https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
        // https://www.postgresql.org/docs/current/runtime-config-preset.html#GUC-MAX-IDENTIFIER-LENGTH
        () ->
            assertDoesNotThrow(
                () ->
                    new Column("a23456789012345678901234567890123456789012345678901234567890123")),
        // invalid: too long (> 63 characters)
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Column(
                        "a234567890123456789012345678901234567890123456789012345678901234")));
  }

  @Test
  void testCompositeKeySeparator() {
    Column pk1 = new Column("id1").setType(ColumnType.STRING);
    Column pk2 = new Column("id2").setType(ColumnType.STRING);
    List<Column> primaryKeys = List.of(pk1, pk2);

    TableMetadata tableMetadata = mock(TableMetadata.class);
    when(tableMetadata.getPrimaryKeyColumns()).thenReturn(primaryKeys);

    Column column = mock(Column.class);
    when(column.getReferences()).thenCallRealMethod();
    when(column.getRefTableName()).thenReturn("MyRefTable");
    when(column.getRefTable()).thenReturn(tableMetadata);
    when(column.getRefLinkColumn()).thenReturn(null);
    when(column.getName()).thenReturn("MyColname");

    when(column.getColumnType()).thenReturn(ColumnType.REF);
    List<String> actualRefs = column.getReferences().stream().map(Reference::getName).toList();
    when(column.getColumnType()).thenReturn(ColumnType.REFBACK);
    List<String> actualRefbacks = column.getReferences().stream().map(Reference::getName).toList();

    assertAll(
        () -> assertEquals(List.of("MyColname.id1", "MyColname.id2"), actualRefs),
        () -> assertEquals(List.of("MyColname-id1", "MyColname-id2"), actualRefbacks));
  }
}
