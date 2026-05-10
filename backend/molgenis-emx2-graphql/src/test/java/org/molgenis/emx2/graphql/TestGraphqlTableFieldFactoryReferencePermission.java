package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlTableFieldFactoryReferencePermission {

  private static final String SCHEMA_NAME = "TGqlTableFieldFactoryRefPerm";
  private static final String TABLE_NAME = "Pet";
  private static final String ROLE_REF_ONLY = "refOnlyRole";
  private static final String USER_REF_ONLY = "TGqlTffRefOnlyUser";
  private static final String ROLE_SELECT_ALL_REF_NONE = "selectAllRefNoneRole";
  private static final String USER_SELECT_ALL_REF_NONE = "TGqlTffSelectAllRefNoneUser";

  private static Database database;
  private static Schema schema;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema.getMetadata().create(table(TABLE_NAME, column("name").setType(STRING).setKey(1)));

    if (!database.hasUser(USER_REF_ONLY)) {
      database.addUser(USER_REF_ONLY);
    }
    if (!database.hasUser(USER_SELECT_ALL_REF_NONE)) {
      database.addUser(USER_SELECT_ALL_REF_NONE);
    }

    SqlRoleManager roleManager = ((SqlDatabase) database).getRoleManager();
    roleManager.createRole(schema, ROLE_REF_ONLY, "");
    roleManager.setPermissions(
        schema,
        ROLE_REF_ONLY,
        new PermissionSet()
            .putTable(
                TABLE_NAME,
                new TablePermission(TABLE_NAME)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.ALL)));
    schema.addMember(USER_REF_ONLY, ROLE_REF_ONLY);

    roleManager.createRole(schema, ROLE_SELECT_ALL_REF_NONE, "");
    roleManager.setPermissions(
        schema,
        ROLE_SELECT_ALL_REF_NONE,
        new PermissionSet()
            .putTable(
                TABLE_NAME,
                new TablePermission(TABLE_NAME)
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE)));
    schema.addMember(USER_SELECT_ALL_REF_NONE, ROLE_SELECT_ALL_REF_NONE);
  }

  @Test
  void referenceOnlyUser_hasReferencePermission_butNotViewPermission() {
    database.setActiveUser(USER_REF_ONLY);
    try {
      TableMetadata tableMeta = schema.getMetadata().getTableMetadata(TABLE_NAME);
      GraphqlTableFieldFactory factory = new GraphqlTableFieldFactory(schema);

      assertTrue(
          factory.hasReferencePermission(tableMeta),
          "User with REFERENCE role should have reference permission on " + TABLE_NAME);
      assertFalse(
          factory.hasViewPermission(tableMeta),
          "User with REFERENCE-only role should not have view permission on " + TABLE_NAME);
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void selectAllReferenceNoneUser_hasReferencePermission_matchingSession_canReference() {
    database.setActiveUser(USER_SELECT_ALL_REF_NONE);
    try {
      TableMetadata tableMeta = schema.getMetadata().getTableMetadata(TABLE_NAME);
      GraphqlTableFieldFactory factory = new GraphqlTableFieldFactory(schema);

      assertTrue(
          factory.hasViewPermission(tableMeta),
          "User with SELECT=ALL should have view permission on " + TABLE_NAME);
      assertTrue(
          factory.hasReferencePermission(tableMeta),
          "User with SELECT=ALL and REFERENCE=NONE should still have reference permission"
              + " (select.allowsRowAccess() implies canReference) on "
              + TABLE_NAME);
    } finally {
      database.becomeAdmin();
    }
  }
}
