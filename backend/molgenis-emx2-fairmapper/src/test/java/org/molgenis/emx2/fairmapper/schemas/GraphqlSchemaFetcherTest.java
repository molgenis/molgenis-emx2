package org.molgenis.emx2.fairmapper.schemas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlClient;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.web.ApiTestBase;

class GraphqlSchemaFetcherTest extends ApiTestBase {

  private static final String SCHEMA_NAME = GraphqlSchemaFetcherTest.class.getSimpleName();
  private static final Database DATABASE = TestDatabaseFactory.getTestDatabase();
  public static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void setupSchema() {
    SchemaMetadata schema = DATABASE.dropCreateSchema(SCHEMA_NAME).getMetadata();
    TableMetadata person =
        schema.create(
            TableMetadata.table("Person").add(Column.column("name", ColumnType.STRING).setPkey()));

    schema.create(
        TableMetadata.table("Product").add(Column.column("name", ColumnType.STRING).setPkey()),
        TableMetadata.table("Order")
            .add(
                Column.column("orderId", ColumnType.INT).setPkey(),
                Column.column("person", ColumnType.REF).setRefTable("Person"),
                Column.column("products", ColumnType.REF_ARRAY).setRefTable("Product")));

    person.add(
        Column.column("orders")
            .setType(ColumnType.REFBACK)
            .setRefTable("Order")
            .setRefBack("person"));
  }

  @Test
  void shouldHandleRealResponse() {
    String token = JWTgenerator.createTemporaryToken(database);
    GraphqlClient graphqlClient = new GraphqlClient("http://localhost:" + port, token);
    GraphqlSchemaFetcher fetcher = new GraphqlSchemaFetcher(graphqlClient);

    Optional<SchemaMetadata> result = fetcher.fetch(SCHEMA_NAME);

    assertTrue(result.isPresent());
    assertMatchesPersonProductOrderSchema(result.get());
  }

  @Test
  void givenGraphqlResponseWithSchema_whenFetch_thenMapsResponseToSchemaMetadata()
      throws IOException {
    GraphqlClient client = staticClient(readSchemaResponse());
    GraphqlSchemaFetcher fetcherWithMock = new GraphqlSchemaFetcher(client);

    Optional<SchemaMetadata> result = fetcherWithMock.fetch(SCHEMA_NAME);

    assertTrue(result.isPresent());
    assertMatchesPersonProductOrderSchema(result.get());
  }

  @Test
  void givenGraphqlResponseWithoutSchemaField_whenFetch_thenThrows()
      throws JsonProcessingException {
    GraphqlClient client = staticClient(MAPPER.readTree("{\"notSchema\": {}}"));
    GraphqlSchemaFetcher fetcherWithMock = new GraphqlSchemaFetcher(client);

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> fetcherWithMock.fetch(SCHEMA_NAME));
    assertTrue(exception.getMessage().contains("No schema returned"));
  }

  private void assertMatchesPersonProductOrderSchema(SchemaMetadata schema) {
    assertEquals(Set.of("Person", "Product", "Order"), schema.getTableNames());

    TableMetadata order = schema.getTableMetadata("Order");
    Column orderId = order.getColumn("orderId");
    assertEquals(ColumnType.INT, orderId.getColumnType());
    assertEquals(1, orderId.getKey());

    Column orderPerson = order.getColumn("person");
    assertEquals(ColumnType.REF, orderPerson.getColumnType());
    assertEquals("Person", orderPerson.getRefTableName());

    Column orderProducts = order.getColumn("products");
    assertEquals(ColumnType.REF_ARRAY, orderProducts.getColumnType());
    assertEquals("Product", orderProducts.getRefTableName());

    TableMetadata personTable = schema.getTableMetadata("Person");
    Column personName = personTable.getColumn("name");
    assertEquals(ColumnType.STRING, personName.getColumnType());
    assertEquals(1, personName.getKey());

    Column personOrders = personTable.getColumn("orders");
    assertEquals(ColumnType.REFBACK, personOrders.getColumnType());
    assertEquals("Order", personOrders.getRefTableName());
    assertEquals("person", personOrders.getRefBack());

    TableMetadata productTable = schema.getTableMetadata("Product");
    Column productName = productTable.getColumn("name");
    assertEquals(ColumnType.STRING, productName.getColumnType());
    assertEquals(1, productName.getKey());
  }

  private GraphqlClient staticClient(JsonNode response) {
    GraphqlClient client = mock(GraphqlClient.class);
    when(client.sendSchemaQuery(anyString(), anyString())).thenReturn(response);
    return client;
  }

  private JsonNode readSchemaResponse() throws IOException {
    try (InputStream inputStream =
        GraphqlSchemaFetcherTest.class.getResourceAsStream("expected-schema.json")) {
      String rawSchema = new String(Objects.requireNonNull(inputStream).readAllBytes());
      return new ObjectMapper().readTree("{\"_schema\":" + rawSchema + "}");
    }
  }
}
