package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestRlsInheritanceCascade {

  private static final String SCHEMA_NAME = "TestRlsInheritanceCascade";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
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

  private long countPoliciesForTable(String tableName) {
    return jooq.fetchOne(
            "SELECT count(*) FROM pg_policies WHERE schemaname = ? AND tablename = ?",
            SCHEMA_NAME,
            tableName)
        .get(0, Long.class);
  }

  private boolean columnExists(String tableName, String columnName) {
    Long count =
        jooq.fetchOne(
                "SELECT count(*) FROM information_schema.columns"
                    + " WHERE table_schema = ? AND table_name = ? AND column_name = ?",
                SCHEMA_NAME,
                tableName,
                columnName)
            .get(0, Long.class);
    return count != null && count > 0L;
  }

  private boolean metadataRlsEnabled(String tableName) {
    return Boolean.TRUE.equals(schema.getTable(tableName).getMetadata().getRlsEnabled());
  }

  @Test
  void enableOnRootEnablesAllExistingChildren() {
    schema.create(table("Person").add(column("fullName").setPkey()));
    schema.create(table("Employee").setInheritName("Person").add(column("salary")));
    schema.create(table("Manager").setInheritName("Employee").add(column("dept")));

    SqlTableMetadata root = (SqlTableMetadata) schema.getTable("Person").getMetadata();
    root.setRlsEnabled(true);

    for (String tableName : new String[] {"Person", "Employee", "Manager"}) {
      assertTrue(columnExists(tableName, "mg_owner"), tableName + " must have mg_owner");
      assertTrue(columnExists(tableName, "mg_groups"), tableName + " must have mg_groups");
      assertEquals(4L, countPoliciesForTable(tableName), tableName + " must have 4 policies");
      assertTrue(tableHasRls(tableName), tableName + " must have RLS enabled");
      assertTrue(metadataRlsEnabled(tableName), tableName + " metadata rls_enabled must be true");
    }
  }

  @Test
  void newChildUnderEnabledRootGetsPolicies() {
    schema.create(table("Person").add(column("fullName").setPkey()));
    SqlTableMetadata root = (SqlTableMetadata) schema.getTable("Person").getMetadata();
    root.setRlsEnabled(true);

    schema.create(table("Employee").setInheritName("Person").add(column("salary")));

    assertTrue(columnExists("Employee", "mg_owner"), "new child must have mg_owner");
    assertTrue(columnExists("Employee", "mg_groups"), "new child must have mg_groups");
    assertEquals(4L, countPoliciesForTable("Employee"), "new child must have 4 policies");
    assertTrue(tableHasRls("Employee"), "new child must have RLS enabled");
    assertTrue(metadataRlsEnabled("Employee"), "new child metadata rls_enabled must be true");
  }

  @Test
  void enableOnNonRootRejected() {
    schema.create(table("Person").add(column("fullName").setPkey()));
    schema.create(table("Employee").setInheritName("Person").add(column("salary")));

    SqlTableMetadata child = (SqlTableMetadata) schema.getTable("Employee").getMetadata();
    MolgenisException ex = assertThrows(MolgenisException.class, () -> child.setRlsEnabled(true));

    assertTrue(ex.getMessage().contains("root"), "message must contain 'root': " + ex.getMessage());
    assertTrue(
        ex.getMessage().contains("Person"),
        "message must contain root name 'Person': " + ex.getMessage());

    assertFalse(
        columnExists("Person", "mg_owner"), "Person must not have mg_owner after rejection");
    assertFalse(
        columnExists("Employee", "mg_owner"), "Employee must not have mg_owner after rejection");
    assertFalse(metadataRlsEnabled("Person"), "Person metadata rls_enabled must remain false");
    assertFalse(metadataRlsEnabled("Employee"), "Employee metadata rls_enabled must remain false");
  }

  @Test
  void disableOnRootCascades() {
    schema.create(table("Person").add(column("fullName").setPkey()));
    schema.create(table("Employee").setInheritName("Person").add(column("salary")));

    SqlTableMetadata root = (SqlTableMetadata) schema.getTable("Person").getMetadata();
    root.setRlsEnabled(true);
    root.setRlsEnabled(false);

    for (String tableName : new String[] {"Person", "Employee"}) {
      assertFalse(tableHasRls(tableName), tableName + " must have RLS disabled");
      assertEquals(0L, countPoliciesForTable(tableName), tableName + " must have 0 policies");
      assertFalse(columnExists(tableName, "mg_owner"), tableName + " must not have mg_owner");
      assertFalse(columnExists(tableName, "mg_groups"), tableName + " must not have mg_groups");
      assertFalse(metadataRlsEnabled(tableName), tableName + " metadata rls_enabled must be false");
    }
  }
}
