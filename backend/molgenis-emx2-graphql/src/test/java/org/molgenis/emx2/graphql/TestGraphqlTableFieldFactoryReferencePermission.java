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

    SqlRoleManager roleManager = ((SqlDatabase) database).getRoleManager();
    roleManager.createRole(SCHEMA_NAME, ROLE_REF_ONLY);
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
}
