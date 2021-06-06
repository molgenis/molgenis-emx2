package org.molgenis.emx2.graphql;

import static org.junit.Assert.fail;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqAdminFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqlAdminFields";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    //    PetStoreExample.create(schema.getMetadata());
    //    PetStoreExample.populate(schema);
  }

  @Test
  public void testUsers() throws IOException {
    try {
      database.setActiveUser("admin");
      Schema schema = database.dropCreateSchema(schemaName);
      grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
      int count = database.countUsers();

      TestCase.assertEquals(
          count, execute("{_admin{userCount}}").at("/_admin/userCount").intValue());

      // test that only admin can do this
      database.setActiveUser(null);
      grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
      try {
        TestCase.assertEquals(null, execute("{_admin{userCount}}").textValue());
        fail("should fail");
      } catch (Exception e) {
        TestCase.assertTrue(e.getMessage().contains("FieldUndefined"));
      }

    } finally {
      database.setActiveUser(null);
    }
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
