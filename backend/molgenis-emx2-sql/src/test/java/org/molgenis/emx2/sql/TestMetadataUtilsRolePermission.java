package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class TestMetadataUtilsRolePermission {

  private static final String SCHEMA_NAME = TestMetadataUtilsRolePermission.class.getSimpleName();
  private static Database db;
  private static DSLContext jooq;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    db.dropCreateSchema(SCHEMA_NAME);
  }

  @Test
  public void rolePermissionMetadataTableExists() {
    org.jooq.Schema molgenisSchema = jooq.meta().getSchemas("MOLGENIS").get(0);
    org.jooq.Table<?> table =
        molgenisSchema.getTables().stream()
            .filter(t -> "role_permission_metadata".equals(t.getName()))
            .findFirst()
            .orElse(null);
    assertNotNull(table, "MOLGENIS.role_permission_metadata must exist after DB init");
  }

  @Test
  public void rolePermissionMetadataHasCorrectColumns() {
    List<Record> columns =
        jooq.fetch(
            "SELECT column_name, data_type, is_nullable, column_default"
                + " FROM information_schema.columns"
                + " WHERE table_schema = 'MOLGENIS'"
                + "   AND table_name = 'role_permission_metadata'"
                + " ORDER BY ordinal_position");

    assertFalse(columns.isEmpty(), "role_permission_metadata must have columns");

    List<String> columnNames =
        columns.stream().map(r -> r.get("column_name", String.class)).toList();

    assertTrue(columnNames.contains("schema_name"), "must have schema_name");
    assertTrue(columnNames.contains("role_name"), "must have role_name");
    assertTrue(columnNames.contains("table_name"), "must have table_name");
    assertTrue(columnNames.contains("select_scope"), "must have select_scope");
    assertTrue(columnNames.contains("insert_scope"), "must have insert_scope");
    assertTrue(columnNames.contains("update_scope"), "must have update_scope");
    assertTrue(columnNames.contains("delete_scope"), "must have delete_scope");
    assertTrue(columnNames.contains("change_owner"), "must have change_owner");
    assertTrue(columnNames.contains("change_group"), "must have change_group");
    assertTrue(columnNames.contains("description"), "must have description");
    assertTrue(columnNames.contains("updated_by"), "must have updated_by");
    assertTrue(columnNames.contains("updated_at"), "must have updated_at");
  }

  @Test
  public void rolePermissionMetadataHasPrimaryKey() {
    List<Record> pkCols =
        jooq.fetch(
            "SELECT kcu.column_name"
                + " FROM information_schema.table_constraints tc"
                + " JOIN information_schema.key_column_usage kcu"
                + "   ON tc.constraint_name = kcu.constraint_name"
                + "  AND tc.table_schema = kcu.table_schema"
                + " WHERE tc.table_schema = 'MOLGENIS'"
                + "   AND tc.table_name = 'role_permission_metadata'"
                + "   AND tc.constraint_type = 'PRIMARY KEY'"
                + " ORDER BY kcu.ordinal_position");

    List<String> pkColumnNames =
        pkCols.stream().map(r -> r.get("column_name", String.class)).toList();
    assertTrue(pkColumnNames.contains("schema_name"), "PK must include schema_name");
    assertTrue(pkColumnNames.contains("role_name"), "PK must include role_name");
    assertTrue(pkColumnNames.contains("table_name"), "PK must include table_name");
  }

  @Test
  public void triggerRejectsUpdateOnSystemRoleRow() {
    String triggerTestUser = "TriggerTestUser";
    if (!db.hasUser(triggerTestUser)) db.addUser(triggerTestUser);

    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, select_scope)"
            + " VALUES (?, 'Owner', 'someTable', 'ALL')",
        SCHEMA_NAME);

    db.setActiveUser(triggerTestUser);
    try {
      assertThrows(
          Exception.class,
          () ->
              jooq.execute(
                  "UPDATE \"MOLGENIS\".role_permission_metadata"
                      + " SET select_scope = 'NONE'"
                      + " WHERE schema_name = ? AND role_name = 'Owner'",
                  SCHEMA_NAME),
          "UPDATE on system role row must be rejected by trigger mg_protect_system_roles");
    } finally {
      db.becomeAdmin();
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'Owner'",
          SCHEMA_NAME);
    }
  }
}
