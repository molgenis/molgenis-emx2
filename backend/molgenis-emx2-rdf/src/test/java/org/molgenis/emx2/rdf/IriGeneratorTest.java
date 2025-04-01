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
  private static final String ENCODED_PRIMARYKEY = "last%20name=van%20de%20achternaam";

  @Test
  void testIRIGenerator() {
    SchemaMetadata schemaMetadata = mock(SchemaMetadata.class);
    when(schemaMetadata.getName()).thenReturn(SCHEMA_NAME);

    Schema schema = mock(Schema.class);
    when(schema.getMetadata()).thenReturn(schemaMetadata);

    TableMetadata tableMetadata = mock(TableMetadata.class);
    when(tableMetadata.getSchemaName()).thenReturn(SCHEMA_NAME);
    when(tableMetadata.getIdentifier()).thenReturn(TABLE_ID);

    Table table = mock(Table.class);
    when(table.getMetadata()).thenReturn(tableMetadata);

    Column column = mock(Column.class);
    when(column.getSchemaName()).thenReturn(SCHEMA_NAME);
    when(column.getTable()).thenReturn(tableMetadata);
    when(column.getIdentifier()).thenReturn(COLUMN_ID);

    PrimaryKey primaryKey = mock(PrimaryKey.class);
    when(primaryKey.getEncodedValue()).thenReturn(ENCODED_PRIMARYKEY);

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
                "http://example.com/my%20Schema/api/rdf/MyTable?last%20name=van%20de%20achternaam",
                rowIRI(baseURL, table, primaryKey).toString()));
  }
}
