package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
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
    PET_STORE.getImportTask(schema, false).run();
    graphQLDatabase = new GraphqlApiFactory().createGraphqlForDatabase(database, taskService);
    graphQLSchema = new GraphqlApiFactory().createGraphqlForSchema(schema);
  }

  @Test
  public void voidTestCreatePermissionGroup() throws IOException {
    executeSchema(
        """
            mutation {
              change(permissions: [
                {
                  groupName: "pet_store_special",
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
                users
                tableName
                tableSchema
                isRowLevel
                hasAdmin
                hasSelect
              }
            }""");
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
