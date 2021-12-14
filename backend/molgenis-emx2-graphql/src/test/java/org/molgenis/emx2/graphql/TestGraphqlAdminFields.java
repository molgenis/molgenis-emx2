package org.molgenis.emx2.graphql;

import static org.junit.Assert.fail;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlAdminFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = TestGraphqlAdminFields.class.getName();

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    //    PetStoreExample.create(schema.getMetadata());
    //    PetStoreExample.populate(schema);
  }

  @Test
  @Ignore
  public void testUsers() throws IOException {
    try {
      database.becomeAdmin();

      // put in transaction so user count is not affected by other operations
      database.tx(
          tdb -> {
            Schema schema = database.dropCreateSchema(schemaName);
            grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);

            int count = database.countUsers();
            try {
              TestCase.assertEquals(
                  count, execute("{_admin{userCount}}").at("/_admin/userCount").intValue());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            // test that only admin can do this
            database.setActiveUser(ANONYMOUS);
            grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);

            try {
              TestCase.assertEquals(null, execute("{_admin{userCount}}").textValue());
              fail("should fail");
            } catch (Exception e) {
              TestCase.assertTrue(e.getMessage().contains("FieldUndefined"));
            }
          });

    } finally {
      database.clearActiveUser();
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
