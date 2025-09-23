package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.io.IOException;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

@TestMethodOrder(OrderAnnotation.class)
public class TestGraphqlPermissions {

  private static GraphQL graphQLSchema;
  private static GraphQL graphQLDatabase;
  private static Database database;
  private static GraphqlSessionHandlerInterface sessionManager;
  private static final String schemaName = "TestGraphqlPermissions";

  // Carries row IDs across ordered tests that depend on prior state.
  private static String orderIdOwnedBySpecial;
  private static String orderIdOwnedByEditor;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    TaskService taskService = new TaskServiceInMemory();
    Schema schema = database.dropCreateSchema(schemaName);
    PET_STORE.getImportTask(schema, true).run();
    graphQLDatabase = new GraphqlApiFactory().createGraphqlForDatabase(database, taskService);
    graphQLSchema = new GraphqlApiFactory().createGraphqlForSchema(schema);

    sessionManager =
        new GraphqlSessionHandlerInterface() {
          private String user;

          @Override
          public void createSession(String username) {
            this.user = username;
          }

          @Override
          public void destroySession() {
            this.user = null;
          }

          @Override
          public String getCurrentUser() {
            return user;
          }
        };

    if (database.hasUser("testViewer")) database.removeUser("testViewer");
    if (database.hasUser("testEditorSpecial")) database.removeUser("testEditorSpecial");
    if (database.hasUser("testEditor")) database.removeUser("testEditor");
  }

  @Test
  @Order(1)
  public void createPermissionGroups() throws IOException {
    executeDb("mutation{signup(email:\"testViewer\",password:\"test123456\"){message}}");
    executeDb("mutation{signup(email:\"testEditorSpecial\",password:\"test123456\"){message}}");
    executeDb("mutation{signup(email:\"testEditor\",password:\"test123456\"){message}}");
    executeSchema(
        """
        mutation {
          change(permissions: [
            {
              groupName: "TestGraphqlPermissions/editorSpecial",
              tableId: "Order",
              isRowLevel: true,
              hasSelect: true, hasInsert: true, hasUpdate:true, hasDelete:true, hasAdmin:false,
              users: ["testEditorSpecial", "someOtherUser@test.com"]
            }
          ]) {
            message
          }
        }""");

    executeSchema(
        """
          mutation {
            change(permissions: [
              {
                groupName: "TestGraphqlPermissions/editorAlsoSpecial",
                tableId: "Order",
                isRowLevel: true,
                hasSelect: true, hasInsert: true, hasUpdate:true, hasDelete:true, hasAdmin:false,
                users: ["someOtherUser@test.com"]
              }
            ]) {
              message
            }
          }""");

    executeSchema(
        """
        mutation {
          change(permissions: [
            {
              groupName: "TestGraphqlPermissions/Editor",
              hasSelect: true, hasInsert: true, hasUpdate:true, hasDelete:true, hasAdmin:false,
              users: ["testEditor"]
            }
          ]) {
            message
          }
        }""");
  }

  @Test
  @Order(2)
  public void verifyEditorSpecialHasTwoUsers() throws IOException {
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
    JsonNode editorSpecial =
        StreamSupport.stream(list.spliterator(), false)
            .filter(
                n -> "TestGraphqlPermissions/editorSpecial".equals(n.path("groupName").asText()))
            .findFirst()
            .orElse(null);

    assertEquals(2, editorSpecial.get("users").size());
  }

  @Test
  @Order(3)
  public void enableRowLevelSecurityAndAssignRowToEditorSpecial() throws IOException {
    executeSchema(
        """
        mutation {
          change(
            tables: [{ name: "Order", rowLevelSecurity: true }]
          ) {
            message
          }
        }""");

    JsonNode orderEditorSpecial =
        executeSchema("query { Order { orderId } }").get("data").get("Order").get(0);

    JsonNode result =
        executeSchema(
            """
        mutation {
          update(
            Order: [
              {
                mg_group: ["TestGraphqlPermissions/editorSpecial"],
                orderId: "%s"
              }
            ]
          ) {
            message
          }
        }"""
                .formatted(orderEditorSpecial.get("orderId").asText()));

    assertEquals(
        "updated 1 records to Order\n", result.get("data").get("update").get("message").asText());

    orderIdOwnedBySpecial = orderEditorSpecial.get("orderId").asText();
  }

  @Test
  @Order(4)
  public void viewerCannotInsert() throws IOException {
    JsonNode result =
        executeDb("mutation{signin(email:\"testViewer\",password:\"test123456\"){message}}");
    database.setActiveUser("testViewer");
    assertEquals("testViewer", database.getActiveUser());
    assertThrows(
        MolgenisException.class,
        () ->
            executeSchema(
                "mutation {insert(Order: {pet: {name: \"pooky\"}, quantity: 5 }) { message }}"));
  }

  @Test
  @Order(5)
  public void editorCanInsertAndOwnRow() throws IOException {
    executeDb("mutation{signin(email:\"testEditor\",password:\"test123456\"){message}}");
    database.setActiveUser("testEditor");
    assertEquals("testEditor", database.getActiveUser());

    executeSchema("mutation {insert(Order: {pet: {name: \"pooky\"}, quantity: 99 }) { message }}");

    JsonNode orderEditor =
        executeSchema("query { Order(filter: {quantity: { equals: 99 }}) { orderId } }")
            .get("data")
            .get("Order")
            .get(0);

    orderIdOwnedByEditor = orderEditor.get("orderId").asText();
  }

  @Test
  @Order(6)
  public void specialEditorCanUpdateOwnRow() throws IOException {
    executeDb("mutation{signin(email:\"testEditorSpecial\",password:\"test123456\"){message}}");
    database.setActiveUser("testEditorSpecial");
    JsonNode result =
        executeSchema(
            """
            mutation {
              update(
                Order: [
                  {
                    orderId: "%s",
                    quantity: 77
                  }
                ]
              ) {
                message
              }
            }"""
                .formatted(orderIdOwnedBySpecial));

    // Should update one row as user is owner of the row
    assertEquals(
        "updated 1 records to Order\n", result.get("data").get("update").get("message").asText());
  }

  @Test
  @Order(7)
  public void specialEditorCannotUpdateEditorsRow() throws IOException {
    JsonNode result =
        executeSchema(
            """
            mutation {
              update(
                Order: [
                  {
                    orderId: "%s",
                    quantity: 77
                  }
                ]
              ) {
                message
              }
            }"""
                .formatted(orderIdOwnedByEditor));

    assertEquals(
        "updated 0 records to Order\n", result.get("data").get("update").get("message").asText());
  }

  @Test
  @Order(8)
  public void specialEditorInsertWithOwnGroupAllowed() throws IOException {
    JsonNode result =
        executeSchema(
            """
        mutation {
          insert(Order: {
            pet: {name: "pooky"},
            quantity: 55,
            mg_group: "TestGraphqlPermissions/editorSpecial"
          }) { message }
        }""");

    assertEquals(
        "inserted 1 records to Order\n", result.get("data").get("insert").get("message").asText());
  }

  @Test
  @Order(9)
  public void specialEditorInsertWithOtherGroupShouldThrow() {
    assertThrows(
        MolgenisException.class,
        () ->
            executeSchema(
                """
                mutation {
                  insert(Order: {
                    pet: {name: "pooky"},
                    quantity: 55,
                    mg_group: [
                      "TestGraphqlPermissions/editorSpecial",
                      "TestGraphqlPermissions/editorAlsoSpecial"
                    ]
                  }) { message }
                }"""));
  }

  @Test
  @Order(10)
  public void specialEditorAddToGroupAndInsert_shouldSucceed() throws IOException {
    database.becomeAdmin();
    executeSchema(
        """
          mutation {
            change(permissions: [
              {
                groupName: "TestGraphqlPermissions/editorAlsoSpecial",
                tableId: "Order",
                isRowLevel: true,
                hasSelect: true, hasInsert: true, hasUpdate:true, hasDelete:true, hasAdmin:false,
                users: ["testEditorSpecial", "someOtherUser@test.com"]
              }
            ]) {
              message
            }
          }""");
    executeDb("mutation{signin(email:\"testEditorSpecial\",password:\"test123456\"){message}}");
    database.setActiveUser("testEditorSpecial");

    JsonNode result =
        executeSchema(
            """
                mutation {
                  insert(Order: {
                    pet: {name: "pooky"},
                    quantity: 55,
                    mg_group: [
                      "TestGraphqlPermissions/editorSpecial",
                      "TestGraphqlPermissions/editorAlsoSpecial"
                    ]
                  }) { message }
                }""");

    assertEquals(
        "inserted 1 records to Order\n", result.get("data").get("insert").get("message").asText());
  }

  @Test
  @Order(11)
  public void specialEditorInsertGlobalShouldThrow() {
    assertThrows(
        MolgenisException.class,
        () ->
            executeSchema(
                "mutation {insert(Order: {pet: {name: \"pooky\"}, quantity: 55 }) { message }}"));
  }

  private JsonNode executeDb(String query) throws IOException {
    Map graphQLContext =
        sessionManager != null
            ? Map.of(GraphqlSessionHandlerInterface.class, sessionManager)
            : Map.of();
    JsonNode result =
        new ObjectMapper()
            .readTree(
                convertExecutionResultToJson(
                    graphQLDatabase.execute(
                        ExecutionInput.newExecutionInput(query)
                            .graphQLContext(graphQLContext)
                            .build())));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }

  private JsonNode executeSchema(String query) throws IOException {
    Map graphQLContext =
        sessionManager != null
            ? Map.of(GraphqlSessionHandlerInterface.class, sessionManager)
            : Map.of();
    JsonNode result =
        new ObjectMapper()
            .readTree(
                convertExecutionResultToJson(
                    graphQLSchema.execute(
                        ExecutionInput.newExecutionInput(query)
                            .graphQLContext(graphQLContext)
                            .build())));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }
}
