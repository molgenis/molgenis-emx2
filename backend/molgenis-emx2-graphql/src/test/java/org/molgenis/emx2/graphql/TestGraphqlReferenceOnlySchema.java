package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlReferenceOnlySchema {

  private static final String SCHEMA_NAME = "TGqlReferenceOnlySchema";
  private static final String TABLE_PARENT = "Parent";
  private static final String TABLE_CHILD = "Child";
  private static final String ROLE_REF_VIEWER = "refViewerRole";
  private static final String USER_REF_VIEWER = "TGqlRefOnlyUser";
  private static final String USER_FULL_VIEWER = "TGqlFullViewUser";

  private static final String INTROSPECT_CHILD_FIELDS =
      "{ __type(name: \"Child\") { fields { name } } }";

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
    roleManager.createRole(schema, ROLE_REF_VIEWER, "");
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

  @AfterAll
  static void tearDown() {
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void referenceOnlyUser_childTypeExposesOnlyPrimaryKeyFields() throws IOException {
    database.setActiveUser(USER_REF_VIEWER);
    try {
      GraphqlExecutor executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
      JsonNode fields = introspectChildFields(executor);

      assertNotNull(fields, "Child type should be present in schema");
      List<String> fieldNames = collectFieldNames(fields);
      assertTrue(fieldNames.contains("id"), "PK field 'id' must be present on thin Child type");
      assertFalse(
          fieldNames.contains("name"),
          "Non-PK field 'name' must NOT be present on thin Child type");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void referenceOnlyUser_parentTypeHasChildField() throws IOException {
    database.setActiveUser(USER_REF_VIEWER);
    try {
      GraphqlExecutor executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
      String introspectParent = "{ __type(name: \"Parent\") { fields { name } } }";
      JsonNode result = executeQuery(executor, introspectParent);
      JsonNode fields = result.at("/__type/fields");
      List<String> fieldNames = collectFieldNames(fields);
      assertTrue(
          fieldNames.contains("child"),
          "Parent type must expose 'child' FK field for ref-only user");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void fullViewerUser_childTypeExposesAllFields() throws IOException {
    database.setActiveUser(USER_FULL_VIEWER);
    try {
      GraphqlExecutor executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
      JsonNode fields = introspectChildFields(executor);

      assertNotNull(fields, "Child type should be present in schema for full viewer");
      List<String> fieldNames = collectFieldNames(fields);
      assertTrue(fieldNames.contains("id"), "PK field 'id' must be present for full viewer");
      assertTrue(
          fieldNames.contains("name"),
          "Non-PK field 'name' must be present for full viewer (full type, not thin)");
    } finally {
      database.becomeAdmin();
    }
  }

  private JsonNode introspectChildFields(GraphqlExecutor executor) throws IOException {
    JsonNode result = executeQuery(executor, INTROSPECT_CHILD_FIELDS);
    return result.at("/__type/fields");
  }

  private JsonNode executeQuery(GraphqlExecutor executor, String query) throws IOException {
    String json =
        GraphqlExecutor.convertExecutionResultToJson(executor.executeWithoutSession(query));
    return new ObjectMapper().readTree(json).get("data");
  }

  private List<String> collectFieldNames(JsonNode fieldsArray) {
    List<String> names = new ArrayList<>();
    if (fieldsArray != null && fieldsArray.isArray()) {
      for (JsonNode field : fieldsArray) {
        names.add(field.get("name").asText());
      }
    }
    return names;
  }
}
