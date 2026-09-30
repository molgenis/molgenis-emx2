package org.molgenis.emx2.fairmapper.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.web.ApiTestBase;

class GraphqlSchemaMetadataProviderTest extends ApiTestBase {

  private static final String SCHEMA_NAME = GraphqlSchemaMetadataProviderTest.class.getSimpleName();

  private GraphqlSchemaMetadataProvider schemaMetadataProvider;
  private SchemaMetadata schema;

  @BeforeEach
  void setupSchema() {
    String token = JWTgenerator.createTemporaryToken(database);
    GraphqlClient client = new GraphqlClient("http://localhost:" + port, token);
    schemaMetadataProvider = new GraphqlSchemaMetadataProvider(client);

    schema = database.dropCreateSchema(SCHEMA_NAME, "GraphQL Database test schema").getMetadata();
    schema.create(
        new TableMetadata("Product")
            .add(
                Column.column("id", ColumnType.INT).setPkey(),
                Column.column("name", ColumnType.STRING)),
        new TableMetadata("Order")
            .add(
                Column.column("id", ColumnType.INT).setPkey(),
                Column.column("products", ColumnType.REF_ARRAY).setRefTable("Product")));
  }

  @Test
  void shouldGetSchemaMetaData() {
    SchemaMetadata retrieved = schemaMetadataProvider.getSchemaMetadata(SCHEMA_NAME);
    org.molgenis.emx2.json.Schema actual = new org.molgenis.emx2.json.Schema(retrieved);
    org.molgenis.emx2.json.Schema expected = new org.molgenis.emx2.json.Schema(schema);
    assertEquals(expected, actual);
  }

  @Test
  void shouldHandleCrossSchemaReferences() {
    String refSchemaName = SCHEMA_NAME + "_ref";
    Schema refSchema = database.dropCreateSchema(refSchemaName);
    TableMetadata supplier =
        refSchema
            .create(
                TableMetadata.table("Supplier")
                    .add(Column.column("name", ColumnType.STRING).setPkey()))
            .getMetadata();

    schema
        .getTableMetadata("Product")
        .add(
            Column.column("supplier", ColumnType.REF)
                .setRefSchemaName(refSchemaName)
                .setRefTable("Supplier"));

    SchemaMetadata schemaMetadata = schemaMetadataProvider.getSchemaMetadata(SCHEMA_NAME);
    TableMetadata product = schemaMetadata.getTableMetadata("Product");

    TableMetadata ref = product.getColumn("supplier").getRefTable();
    org.molgenis.emx2.json.Table actual = new org.molgenis.emx2.json.Table(ref);

    org.molgenis.emx2.json.Table expected = new org.molgenis.emx2.json.Table(supplier);
    assertEquals(expected, actual);
  }

  @Test
  void shouldThrowWhenNonExistingSchema() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> schemaMetadataProvider.getSchemaMetadata("non-existing"));
    assertEquals(
        "\"Schema 'non-existing' unknown. Might you need to sign in or ask permission?\"",
        exception.getMessage());
  }

  @Test
  void givenResponse_whenNoSchemaReturned_thenThrow() throws IOException {
    GraphqlSchemaMetadataProvider provider = mockClient("{}");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> provider.getSchemaMetadata(SCHEMA_NAME));
    assertEquals("No schema returned in graphql response: {}", exception.getMessage());
  }

  @Test
  void givenResponse_whenInvalidSchemaReturned_thenThrow() throws IOException {
    GraphqlSchemaMetadataProvider provider = mockClient("{\"_schema\": \"not-an-object\"}");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> provider.getSchemaMetadata(SCHEMA_NAME));
    assertTrue(
        exception
            .getMessage()
            .startsWith("Unable to map query result to SchemaMetaData: \"not-an-object\""));
  }

  private GraphqlSchemaMetadataProvider mockClient(String responseJson) throws IOException {
    GraphqlClient client = mock(GraphqlClient.class);
    when(client.sendSchemaQuery(any(), any()))
        .thenReturn(new ObjectMapper().readTree(responseJson));
    return new GraphqlSchemaMetadataProvider(client);
  }
}
