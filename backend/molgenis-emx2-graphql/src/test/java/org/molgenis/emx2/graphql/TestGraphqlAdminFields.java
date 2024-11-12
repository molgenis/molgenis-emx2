package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlAdminFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = TestGraphqlAdminFields.class.getSimpleName();

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testUsers() {
    // put in transaction so user count is not affected by other operations
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          Schema schema = tdb.dropCreateSchema(schemaName);
          grapql = new GraphqlApiFactory().createGraphqlForDatabase(tdb, null);

          try {
            JsonNode result = execute("{_admin{users{email} userCount}}");
            assertTrue(result.at("/_admin/userCount").intValue() > 0);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          // test that only admin can do this
          tdb.setActiveUser(ANONYMOUS);
          grapql = new GraphqlApiFactory().createGraphqlForDatabase(tdb, null);

          try {
            assertEquals(null, execute("{_admin{userCount}}").textValue());
          } catch (Exception e) {
            assertTrue(e.getMessage().contains("FieldUndefined"));
          }
          tdb.becomeAdmin();
        });
  }
  @Test
  public void testUpdateUser() {
    database.tx(
        testDatabase -> {
          testDatabase.becomeAdmin();
          testDatabase.dropCreateSchema(schemaName);
          GraphQL graphql = new GraphqlApiFactory().createGraphqlForDatabase(testDatabase, null);

          try {
            String query =
                "mutation updateUser($updateUser:InputUpdateUser) {updateUser(updateUser:$updateUser){status, message}}";
            Map<String, Object> variables = getUpdateUser();
            ExecutionInput build =
                ExecutionInput.newExecutionInput().query(query).variables(variables).build();
            String queryResult = convertExecutionResultToJson(graphql.execute(build));
            JsonNode node = new ObjectMapper().readTree(queryResult);
            if (node.get("errors") != null) {
              throw new MolgenisException(node.get("errors").get(0).get("message").asText());
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @NotNull
  private static Map<String, Object> getUpdateUser() {
    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> updateUser = new HashMap<>();
    updateUser.put("email", "testPersoon");
    updateUser.put("enabled", "true");
    //    updateUser.put("revokedRoles", "[]");
    //    updateUser.put("roles", "[]");
    //            updateUser.put("password", "12345678");
    variables.put("updateUser", updateUser);
    return variables;
  }

  @Test
  public void testDeleteUser() {}

  @Test
  public void addUpdateUser() {}

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
