package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlReferenceOnlyTopLevelSuppression {

  private static final String SCHEMA_NAME = "TGqlRefOnlyTopLevel";
  private static final String TABLE_PARENT = "Parent";
  private static final String TABLE_CHILD = "Child";
  private static final String ROLE_REF_VIEWER = "refViewerTopRole";
  private static final String USER_REF_VIEWER = "TGqlRefOnlyTopUser";
  private static final String USER_FULL_VIEWER = "TGqlFullViewTopUser";

  private static final String INTROSPECT_QUERY_FIELDS =
      "{ __type(name: \"Query\") { fields { name } } }";
  private static final String INTROSPECT_MUTATION_ARGS =
      "{ __type(name: \"Save\") { fields { name args { name } } } }";

  private static Database database;
  private static Schema schema;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);

    schema
        .getMetadata()
        .create(
            table(TABLE_CHILD)
                .add(column("id").setType(STRING).setKey(1))
                .add(column("name").setType(STRING)));

    schema
        .getMetadata()
        .create(
            table(TABLE_PARENT)
                .add(column("id").setType(STRING).setKey(1))
                .add(column("child").setType(REF).setRefTable(TABLE_CHILD)));

    if (!database.hasUser(USER_REF_VIEWER)) {
      database.addUser(USER_REF_VIEWER);
    }
    if (!database.hasUser(USER_FULL_VIEWER)) {
      database.addUser(USER_FULL_VIEWER);
    }

    SqlRoleManager roleManager = ((SqlDatabase) database).getRoleManager();
    roleManager.createRole(SCHEMA_NAME, ROLE_REF_VIEWER);
    roleManager.setPermissions(
        schema,
        ROLE_REF_VIEWER,
        new PermissionSet()
            .putTable(
                TABLE_PARENT,
                new TablePermission(TABLE_PARENT)
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE))
            .putTable(
                TABLE_CHILD,
                new TablePermission(TABLE_CHILD)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.ALL)));

    schema.addMember(USER_REF_VIEWER, ROLE_REF_VIEWER);
    schema.addMember(USER_FULL_VIEWER, "Viewer");
  }

  @Test
  void referenceOnlyUser_childIsAbsentFromTopLevelQuery() throws IOException {
    database.setActiveUser(USER_REF_VIEWER);
    try {
      Schema freshSchema = database.getSchema(SCHEMA_NAME);
      GraphqlExecutor executor = new GraphqlExecutor(freshSchema, new TaskServiceInMemory());
      List<String> queryFields = collectQueryFieldNames(executor);

      assertTrue(queryFields.contains("Parent"), "Parent must appear in top-level Query");
      assertFalse(
          queryFields.contains("Child"),
          "Child must NOT appear in top-level Query for reference-only user");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void referenceOnlyUser_childAggAbsentFromTopLevelQuery() throws IOException {
    database.setActiveUser(USER_REF_VIEWER);
    try {
      Schema freshSchema = database.getSchema(SCHEMA_NAME);
      GraphqlExecutor executor = new GraphqlExecutor(freshSchema, new TaskServiceInMemory());
      List<String> queryFields = collectQueryFieldNames(executor);

      assertTrue(queryFields.contains("Parent_agg"), "Parent_agg must appear for view user");
      assertFalse(
          queryFields.contains("Child_agg"),
          "Child_agg must NOT appear in top-level Query for reference-only user");
      assertFalse(
          queryFields.contains("Child_groupBy"),
          "Child_groupBy must NOT appear in top-level Query for reference-only user");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void referenceOnlyUser_childAbsentFromMutationArguments() throws IOException {
    database.setActiveUser(USER_REF_VIEWER);
    try {
      Schema freshSchema = database.getSchema(SCHEMA_NAME);
      GraphqlExecutor executor = new GraphqlExecutor(freshSchema, new TaskServiceInMemory());
      List<String> insertArgs = collectMutationArgNames(executor, "insert");
      List<String> updateArgs = collectMutationArgNames(executor, "update");
      List<String> saveArgs = collectMutationArgNames(executor, "save");
      List<String> deleteArgs = collectMutationArgNames(executor, "delete");

      assertFalse(
          insertArgs.contains("Child"),
          "Child must NOT be an argument of insert for reference-only user");
      assertFalse(
          updateArgs.contains("Child"),
          "Child must NOT be an argument of update for reference-only user");
      assertFalse(
          saveArgs.contains("Child"),
          "Child must NOT be an argument of save for reference-only user");
      assertFalse(
          deleteArgs.contains("Child"),
          "Child must NOT be an argument of delete for reference-only user");

      assertTrue(insertArgs.contains("Parent"), "Parent must be an argument of insert");
      assertTrue(updateArgs.contains("Parent"), "Parent must be an argument of update");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void fullViewerUser_childAppearsAtTopLevelAndInMutations() throws IOException {
    database.setActiveUser(USER_FULL_VIEWER);
    try {
      Schema freshSchema = database.getSchema(SCHEMA_NAME);
      GraphqlExecutor executor = new GraphqlExecutor(freshSchema, new TaskServiceInMemory());
      List<String> queryFields = collectQueryFieldNames(executor);

      assertTrue(queryFields.contains("Child"), "Child must appear in top-level Query for viewer");
      assertTrue(
          queryFields.contains("Child_agg"), "Child_agg must appear in top-level Query for viewer");
      assertTrue(
          queryFields.contains("Child_groupBy"),
          "Child_groupBy must appear in top-level Query for viewer");

      List<String> insertArgs = collectMutationArgNames(executor, "insert");
      assertTrue(insertArgs.contains("Child"), "Child must be an argument of insert for viewer");
    } finally {
      database.becomeAdmin();
    }
  }

  private List<String> collectQueryFieldNames(GraphqlExecutor executor) throws IOException {
    JsonNode result = executeQuery(executor, INTROSPECT_QUERY_FIELDS);
    return collectNames(result.at("/__type/fields"));
  }

  private List<String> collectMutationArgNames(GraphqlExecutor executor, String mutationField)
      throws IOException {
    JsonNode result = executeQuery(executor, INTROSPECT_MUTATION_ARGS);
    JsonNode fields = result.at("/__type/fields");
    if (fields == null || !fields.isArray()) {
      return List.of();
    }
    for (JsonNode field : fields) {
      if (mutationField.equals(field.get("name").asText())) {
        return collectNames(field.get("args"));
      }
    }
    return List.of();
  }

  private JsonNode executeQuery(GraphqlExecutor executor, String query) throws IOException {
    String json =
        GraphqlExecutor.convertExecutionResultToJson(executor.executeWithoutSession(query));
    return new ObjectMapper().readTree(json).get("data");
  }

  private List<String> collectNames(JsonNode array) {
    List<String> names = new ArrayList<>();
    if (array != null && array.isArray()) {
      for (JsonNode item : array) {
        names.add(item.get("name").asText());
      }
    }
    return names;
  }
}
