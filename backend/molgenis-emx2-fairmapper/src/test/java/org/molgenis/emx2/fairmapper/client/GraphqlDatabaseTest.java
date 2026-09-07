package org.molgenis.emx2.fairmapper.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaInfo;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.web.ApiTestBase;

class GraphqlDatabaseTest extends ApiTestBase {

  private static final String SCHEMA_NAME = GraphqlDatabaseTest.class.getSimpleName();
  private static String token;

  @BeforeAll
  static void setup() {
    token = JWTgenerator.createTemporaryToken(database);
    database.dropCreateSchema(SCHEMA_NAME);
  }

  @Test
  void shouldReturnSchemaNamesFromRealServer() {
    GraphqlClient client = new GraphqlClient("http://localhost:" + port, token);
    GraphqlDatabase graphqlDatabase = new GraphqlDatabase(client);

    Collection<String> schemaNames = graphqlDatabase.getSchemaNames();

    assertTrue(schemaNames.contains(SCHEMA_NAME));
  }

  @Test
  void shouldReturnSchemaInfosFromRealServer() {
    GraphqlClient client = new GraphqlClient("http://localhost:" + port, token);
    GraphqlDatabase graphqlDatabase = new GraphqlDatabase(client);

    Collection<SchemaInfo> schemaInfos = graphqlDatabase.getSchemaInfos();

    assertTrue(schemaInfos.stream().anyMatch(info -> SCHEMA_NAME.equals(info.tableSchema())));
  }

  @Test
  void shouldReturnSchemaInfoByNameFromRealServer() {
    GraphqlClient client = new GraphqlClient("http://localhost:" + port, token);
    GraphqlDatabase graphqlDatabase = new GraphqlDatabase(client);

    SchemaInfo actual = graphqlDatabase.getSchemaInfo(SCHEMA_NAME);
    SchemaInfo expected = new SchemaInfo(SCHEMA_NAME, "");
    assertEquals(expected, actual);
  }
}
