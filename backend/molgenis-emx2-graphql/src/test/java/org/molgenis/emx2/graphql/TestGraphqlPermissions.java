package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import graphql.GraphQL;
import java.io.IOException;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class TestGraphqlPermissions {

  private static GraphQL graphQLSchema;
  private static GraphQL graphQLDatabase;
  private static Database database;
  private static final String schemaName = "TestGraphqlPermissions";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    TaskService taskService = new TaskServiceInMemory();
    Schema schema = database.dropCreateSchema(schemaName);
    PET_STORE.getImportTask(schema, true).run();
    graphQLDatabase = new GraphqlApiFactory().createGraphqlForDatabase(database, taskService);
    graphQLSchema = new GraphqlApiFactory().createGraphqlForSchema(schema);
  }

  @Test
  @Order(1)
  public void testCreatePermissionGroup() throws IOException {
    executeSchema(
        """
            mutation {
              change(permissions: [
                {
                  groupName: "TestGraphqlPermissions/specialEditor",
                  tableId: "Order",
                  isRowLevel: true,
                  hasSelect: true, hasInsert: true, hasUpdate:true, hasDelete:true, hasAdmin:false,
                  users: ["test@test.com", "test2@test.com"]
                }
              ]) {
                message
              }
            }""");

    JsonNode permissions =
        executeDb(
            """
            query {
              _permissions {
                groupName
                users
                tableName
                tableSchema
                isRowLevel
                hasAdmin
                hasSelect
              }
            }""");

    ArrayNode list = (ArrayNode) permissions.path("data").path("_permissions");
    JsonNode specialPermission =
        StreamSupport.stream(list.spliterator(), false)
            .filter(
                n -> "TestGraphqlPermissions/specialEditor".equals(n.path("groupName").asText()))
            .findFirst()
            .orElse(null);

    assertEquals(2, specialPermission.get("users").size());
  }

  @Test
  @Order(2)
  public void testRowLevelSecurity() throws IOException {
    executeSchema(
        """
                  mutation {
                    change(
                      tables: [{ name: "Order", rowLevelSecurity: true }]
                    ) {
                      message
                    }
                  }
                  """);

    JsonNode order = executeSchema("query { Order { orderId } }").get("data").get("Order").get(0);

    JsonNode message =
        executeSchema(
            """
                mutation {
                  update(
                    Order: [
                      {
                        mg_group: ["TestGraphqlPermissions/specialEditor"],
                        orderId: "%s"
                      }
                    ]
                  ) {
                    message
                  }
                }
                """
                .formatted(order.get("orderId").asText()));

    String test = "Test";
  }

  private JsonNode executeSchema(String query) throws IOException {
    JsonNode result =
        new ObjectMapper().readTree(convertExecutionResultToJson(graphQLSchema.execute(query)));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }

  private JsonNode executeDb(String query) throws IOException {
    JsonNode result =
        new ObjectMapper().readTree(convertExecutionResultToJson(graphQLDatabase.execute(query)));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }
}
