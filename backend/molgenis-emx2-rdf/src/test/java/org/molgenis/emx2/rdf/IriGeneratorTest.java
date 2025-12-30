package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.rdf.IriGenerator.columnIRI;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;
import static org.molgenis.emx2.rdf.IriGenerator.schemaIRI;
import static org.molgenis.emx2.rdf.IriGenerator.tableIRI;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

class IriGeneratorTest {
  private static final String baseURL = "http://example.com";
  private static final String SCHEMA_NAME = "my Schema";
  private static final String TABLE_NAME = "my Table";
  private static final String TABLE_ID = "MyTable";
  private static final String COLUMN_NAME = "my Column";
  private static final String COLUMN_ID = "myColumn";
  private static final String ENCODED_PRIMARYKEY = "lastName=van%20de%20achternaam";

  private static final String CHILD_SCHEMA_NAME = "my Child Schema";
  private static final String CHILD_TABLE_NAME = "my Child Table";
  private static final String CHILD_TABLE_ID = "MyChildTable";
  private static final String CHILD_COLUMN_NAME = "my Child Column";
  private static final String CHILD_COLUMN_ID = "myChildColumn";

  @Test
  void testIRIGenerator() {
    // Primary test data
    SchemaMetadata schemaMetadata = mock(SchemaMetadata.class);
    when(schemaMetadata.getName()).thenReturn(SCHEMA_NAME);

    Schema schema = mock(Schema.class);
    when(schema.getMetadata()).thenReturn(schemaMetadata);

    TableMetadata tableMetadata = mock(TableMetadata.class);
    when(tableMetadata.getSchemaName()).thenReturn(SCHEMA_NAME);
    when(tableMetadata.getIdentifier()).thenReturn(TABLE_ID);
    when(tableMetadata.getRootTable()).thenReturn(tableMetadata);

    Table table = mock(Table.class);
    when(table.getMetadata()).thenReturn(tableMetadata);

    Column column = mock(Column.class);
    when(column.getSchemaName()).thenReturn(SCHEMA_NAME);
    when(column.getTable()).thenReturn(tableMetadata);
    when(column.getIdentifier()).thenReturn(COLUMN_ID);

    PrimaryKey primaryKey = mock(PrimaryKey.class);
    when(primaryKey.getEncodedString()).thenReturn(ENCODED_PRIMARYKEY);

    // Test data for checking table inheritance
    SchemaMetadata childSchemaMetadata = mock(SchemaMetadata.class);
    when(childSchemaMetadata.getName()).thenReturn(CHILD_SCHEMA_NAME);

    Schema childSchema = mock(Schema.class);
    when(childSchema.getMetadata()).thenReturn(childSchemaMetadata);

    TableMetadata childTableMetadata = mock(TableMetadata.class);
    when(childTableMetadata.getSchemaName()).thenReturn(CHILD_SCHEMA_NAME);
    when(childTableMetadata.getIdentifier()).thenReturn(CHILD_TABLE_ID);
    when(childTableMetadata.getRootTable()).thenReturn(tableMetadata); // return parent!

    Table childTable = mock(Table.class);
    when(childTable.getMetadata()).thenReturn(childTableMetadata);

    Column childColumn = mock(Column.class);
    when(childColumn.getSchemaName()).thenReturn(CHILD_SCHEMA_NAME);
    when(childColumn.getTable()).thenReturn(childTableMetadata);
    when(childColumn.getIdentifier()).thenReturn(CHILD_COLUMN_ID);

    assertAll(
        () ->
            assertEquals(
                "http://example.com/my%20Schema/api/rdf", schemaIRI(baseURL, schema).toString()),
        () ->
            assertEquals(
                "http://example.com/my%20Schema/api/rdf/MyTable",
                tableIRI(baseURL, table).toString()),
        () ->
            assertEquals(
                "http://example.com/my%20Schema/api/rdf/MyTable/column/myColumn",
                columnIRI(baseURL, column).toString()),
        () ->
            assertEquals(
                "http://example.com/my%20Schema/api/rdf/MyTable/lastName=van%20de%20achternaam",
                rowIRI(baseURL, table, primaryKey).toString()),
        () -> // PrimaryKey is only part that can differ due to root table overriding current table!
        assertEquals(
                "http://example.com/my%20Schema/api/rdf/MyTable/lastName=van%20de%20achternaam",
                rowIRI(baseURL, childTable, primaryKey).toString()));
  }
}
