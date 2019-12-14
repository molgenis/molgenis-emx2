package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.web.GraphqlApi.convertExecutionResultToJson;

public class TestGraphqlDatabaseFields {

  private static GraphQL grapql;
  private static final String schemaName = "TestGraphqlApiForDatabase";

  @BeforeClass
  public static void setup() {
    Database database = DatabaseFactory.getTestDatabase();
    Schema schema = database.createSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    grapql = GraphqlApi.graphqlForDatabase(database);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    int length = execute("{Schemas{name}}").at("/data/Schemas").size();
    execute("mutation{createSchema(name:\"" + schemaName + "B\"){detail}}");
    assertEquals(length + 1, execute("{Schemas{name}}").at("/data/Schemas").size());
    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){detail}}");
    assertEquals(length, execute("{Schemas{name}}").at("/data/Schemas").size());
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper().readTree(convertExecutionResultToJson(grapql.execute(query)));
  }
}
