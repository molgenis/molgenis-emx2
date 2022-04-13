package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

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
  public void testUsers() {
    // put in transaction so user count is not affected by other operations
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          Schema schema = tdb.dropCreateSchema(schemaName);
          grapql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);

          try {
            JsonNode result = execute("{_admin{users{username} userCount}}");
            TestCase.assertTrue(result.at("/_admin/userCount").intValue() > 0);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          // test that only admin can do this
          tdb.setActiveUser(ANONYMOUS);
          grapql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);

          try {
            TestCase.assertEquals(null, execute("{_admin{userCount}}").textValue());
          } catch (Exception e) {
            TestCase.assertTrue(e.getMessage().contains("FieldUndefined"));
          }
          tdb.becomeAdmin();
        });
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
