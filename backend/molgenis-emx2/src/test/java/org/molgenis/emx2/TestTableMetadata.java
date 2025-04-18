package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class TestTableMetadata {
  @Test
  public void validTableMetadataName() {
    assertAll(
        // valid: 1 or more legal characters
        () -> assertDoesNotThrow(() -> new TableMetadata("a")),
        // valid: a space
        () -> assertDoesNotThrow(() -> new TableMetadata("first name")),
        // valid: space & underscore but not next to each other
        () -> assertDoesNotThrow(() -> new TableMetadata("yet_another name")),
        // invalid: # should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("#first name")),
        // invalid: '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first_  name")),
        // invalid: ' _' not allowed
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first   _name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first  _  name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("first  __   name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new TableMetadata("aa    ____      ")),
        // valid: max length (= 31 characters) -> so it fits in Excel sheet names
        () -> assertDoesNotThrow(() -> new TableMetadata("a234567890123456789012345678901")),
        // invalid: too long (> 31 characters)
        () ->
            assertThrows(
                MolgenisException.class,
                () -> new TableMetadata("a2345678901234567890123456789012")));
  }

  @Test
  void testRetrieveColumnByIdentifier() {
    SchemaMetadata schema = mock(SchemaMetadata.class);
    when(schema.getName()).thenReturn("schema name");

    // Columns that would result in an identical identifier are not allowed so do not need to be
    // validated.
    Column c1 = new Column("a colname");
    Column c2 = new Column("a colName");
    TableMetadata table = TableMetadata.table("table name", c1, c2);

    c1.setTable(table);
    c2.setTable(table);
    table.setSchema(schema);

    assertEquals(c2, table.getColumnByIdentifier("aColName"));
  }
}
