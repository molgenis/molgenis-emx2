package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestRlsLifecycle {

  private static final String SCHEMA_NAME = "TestRlsLifecycle";
  private static final String TABLE_ONE = "TableOne";
  private static final String GROUP_ALPHA = "groupAlpha";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_ONE).add(column("id").setPkey()).add(column("val")));
    roleManager.createGroup(schema, GROUP_ALPHA);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private boolean tableHasRls(String tableName) {
    Boolean result =
        jooq.fetchOne(
                "SELECT c.relrowsecurity FROM pg_class c"
                    + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                    + " WHERE n.nspname = ? AND c.relname = ?",
                SCHEMA_NAME,
                tableName)
            .get(0, Boolean.class);
    return Boolean.TRUE.equals(result);
  }

  @Test
  void tableStartsNonRls() {
    assertFalse(tableHasRls(TABLE_ONE), "Table must not have RLS before any custom-role grant");
  }
}
