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

class TestGraphqlCrossSchemaReferencePermission {

  private static final String SCHEMA_A = "TGqlCrossA";
  private static final String SCHEMA_B = "TGqlCrossB";
  private static final String TABLE_PARENT = "Parent";
  private static final String TABLE_CHILD = "Child";
  private static final String TABLE_OTHER = "Other";
  private static final String COL_NAME = "name";
  private static final String COL_ID = "id";
  private static final String COL_CHILD = "child";
  private static final String ROLE_PARENT_VIEWER = "parentViewerRole";
  private static final String ROLE_CHILD_REF_ONLY = "childRefOnlyRole";
  private static final String ROLE_CHILD_FULL_VIEWER = "childFullViewerRole";
  private static final String ROLE_OTHER_REF_ONLY = "otherRefOnlyRole";
  private static final String USER_CROSS_REF = "TGqlCrossRefUser";
  private static final String USER_CROSS_FULL = "TGqlCrossFullUser";
  private static final String USER_CROSS_NONE = "TGqlCrossNoneUser";
  private static final String USER_CROSS_SYS_VIEWER = "TGqlCrossSysViewUser";

  private static final String INTROSPECT_PARENT_FIELDS =
      "{ __type(name: \"Parent\") { fields { name } } }";
  private static final String INTROSPECT_CHILD_FIELDS =
      "{ __type(name: \"TGqlCrossB_Child\") { fields { name } } }";

  private static Database database;
  private static Schema schemaA;
  private static Schema schemaB;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_A);
    database.dropSchemaIfExists(SCHEMA_B);

    schemaB = database.createSchema(SCHEMA_B);
    schemaA = database.createSchema(SCHEMA_A);

    schemaB
        .getMetadata()
        .create(
            table(TABLE_CHILD)
                .add(column(COL_ID).setType(STRING).setKey(1))
                .add(column(COL_NAME).setType(STRING)));

    schemaB.getMetadata().create(table(TABLE_OTHER).add(column(COL_ID).setType(STRING).setKey(1)));

    schemaA
        .getMetadata()
        .create(
            table(TABLE_PARENT)
                .add(column(COL_ID).setType(STRING).setKey(1))
                .add(
                    column(COL_CHILD)
                        .setType(REF)
                        .setRefSchemaName(SCHEMA_B)
                        .setRefTable(TABLE_CHILD)));

    for (String user :
        new String[] {USER_CROSS_REF, USER_CROSS_FULL, USER_CROSS_NONE, USER_CROSS_SYS_VIEWER}) {
      if (!database.hasUser(user)) {
        database.addUser(user);
      }
    }

    SqlRoleManager roleManager = ((SqlDatabase) database).getRoleManager();

    roleManager.createRole(schemaA, ROLE_PARENT_VIEWER, "");
    roleManager.setPermissions(
        schemaA,
        ROLE_PARENT_VIEWER,
        new PermissionSet()
            .putTable(
                TABLE_PARENT,
                new TablePermission(TABLE_PARENT)
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE)));

    roleManager.createRole(schemaB, ROLE_CHILD_REF_ONLY, "");
    roleManager.setPermissions(
        schemaB,
        ROLE_CHILD_REF_ONLY,
        new PermissionSet()
            .putTable(
                TABLE_CHILD,
                new TablePermission(TABLE_CHILD)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.ALL)));

    roleManager.createRole(schemaB, ROLE_CHILD_FULL_VIEWER, "");
    roleManager.setPermissions(
        schemaB,
        ROLE_CHILD_FULL_VIEWER,
        new PermissionSet()
            .putTable(
                TABLE_CHILD,
                new TablePermission(TABLE_CHILD)
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE)));

    roleManager.createRole(schemaB, ROLE_OTHER_REF_ONLY, "");
    roleManager.setPermissions(
        schemaB,
        ROLE_OTHER_REF_ONLY,
        new PermissionSet()
            .putTable(
                TABLE_OTHER,
                new TablePermission(TABLE_OTHER)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.ALL)));

    schemaA.addMember(USER_CROSS_REF, ROLE_PARENT_VIEWER);
    schemaB.addMember(USER_CROSS_REF, ROLE_CHILD_REF_ONLY);

    schemaA.addMember(USER_CROSS_FULL, ROLE_PARENT_VIEWER);
    schemaB.addMember(USER_CROSS_FULL, ROLE_CHILD_FULL_VIEWER);

    schemaA.addMember(USER_CROSS_NONE, ROLE_PARENT_VIEWER);
    schemaB.addMember(USER_CROSS_NONE, ROLE_OTHER_REF_ONLY);

    schemaA.addMember(USER_CROSS_SYS_VIEWER, ROLE_PARENT_VIEWER);
    schemaB.addMember(USER_CROSS_SYS_VIEWER, "Viewer");
  }

  @AfterAll
  static void tearDown() {
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_A);
    database.dropSchemaIfExists(SCHEMA_B);
  }

  @Test
  void crossSchema_referenceOnlyOnChild_emitsThinType() throws IOException {
    database.setActiveUser(USER_CROSS_REF);
    try {
      Schema freshA = database.getSchema(SCHEMA_A);
      GraphqlExecutor executor = new GraphqlExecutor(freshA, new TaskServiceInMemory());

      JsonNode parentFields = queryFields(executor, INTROSPECT_PARENT_FIELDS);
      assertNotNull(parentFields, "Parent type must be present");
      assertTrue(
          collectNames(parentFields).contains(COL_CHILD),
          "Parent.child must be emitted for reference-only user on Child");

      JsonNode childFields = queryFields(executor, INTROSPECT_CHILD_FIELDS);
      assertNotNull(childFields, "Child type must be present for reference-only user");
      List<String> childFieldNames = collectNames(childFields);
      assertTrue(childFieldNames.contains(COL_ID), "PK field 'id' must be present on thin type");
      assertFalse(
          childFieldNames.contains(COL_NAME),
          "Non-PK field 'name' must NOT be present on thin Child type");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void crossSchema_fullViewOnChild_emitsFullType() throws IOException {
    database.setActiveUser(USER_CROSS_FULL);
    try {
      Schema freshA = database.getSchema(SCHEMA_A);
      GraphqlExecutor executor = new GraphqlExecutor(freshA, new TaskServiceInMemory());

      JsonNode parentFields = queryFields(executor, INTROSPECT_PARENT_FIELDS);
      assertTrue(
          collectNames(parentFields).contains(COL_CHILD),
          "Parent.child must be emitted for full-view user");

      JsonNode childFields = queryFields(executor, INTROSPECT_CHILD_FIELDS);
      assertNotNull(childFields, "Child type must be present for full-view user");
      List<String> childFieldNames = collectNames(childFields);
      assertTrue(childFieldNames.contains(COL_ID), "PK field 'id' must be present");
      assertTrue(
          childFieldNames.contains(COL_NAME),
          "Non-PK field 'name' must be present on full Child type");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void crossSchema_noPermissionOnChild_omitsField() throws IOException {
    database.setActiveUser(USER_CROSS_NONE);
    try {
      Schema freshA = database.getSchema(SCHEMA_A);
      GraphqlExecutor executor = new GraphqlExecutor(freshA, new TaskServiceInMemory());

      JsonNode parentFields = queryFields(executor, INTROSPECT_PARENT_FIELDS);
      assertNotNull(parentFields, "Parent type must be present");
      assertFalse(
          collectNames(parentFields).contains(COL_CHILD),
          "Parent.child must be omitted when user has no VIEW or REFERENCE on Child");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void crossSchema_systemViewerInRefSchema_emitsFullType() throws IOException {
    database.setActiveUser(USER_CROSS_SYS_VIEWER);
    try {
      Schema freshA = database.getSchema(SCHEMA_A);
      GraphqlExecutor executor = new GraphqlExecutor(freshA, new TaskServiceInMemory());

      JsonNode parentFields = queryFields(executor, INTROSPECT_PARENT_FIELDS);
      assertTrue(
          collectNames(parentFields).contains(COL_CHILD),
          "Parent.child must be emitted when user is system Viewer in schema B");

      JsonNode childFields = queryFields(executor, INTROSPECT_CHILD_FIELDS);
      assertNotNull(childFields, "Child type must be present for system viewer");
      List<String> childFieldNames = collectNames(childFields);
      assertTrue(childFieldNames.contains(COL_ID), "PK field 'id' must be present");
      assertTrue(
          childFieldNames.contains(COL_NAME),
          "Non-PK field 'name' must be present — system Viewer implies full type");
    } finally {
      database.becomeAdmin();
    }
  }

  private JsonNode queryFields(GraphqlExecutor executor, String query) throws IOException {
    String json =
        GraphqlExecutor.convertExecutionResultToJson(executor.executeWithoutSession(query));
    JsonNode data = new ObjectMapper().readTree(json).get("data");
    return data.at("/__type/fields");
  }

  private List<String> collectNames(JsonNode fieldsArray) {
    List<String> names = new ArrayList<>();
    if (fieldsArray != null && fieldsArray.isArray()) {
      for (JsonNode field : fieldsArray) {
        names.add(field.get("name").asText());
      }
    }
    return names;
  }
}
