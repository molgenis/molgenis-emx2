package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class MetadataUtilsRolePermissionTest {

  private static final String SCHEMA_NAME = MetadataUtilsRolePermissionTest.class.getSimpleName();
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
  public void wildcardTableNameDoesNotRequireTableMetadataRow() {
    List<Record> wildcardRows =
        jooq.fetch(
            "SELECT role_name, table_name FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND table_name = '*'",
            SCHEMA_NAME);
    assertFalse(wildcardRows.isEmpty(), "wildcard rows with table_name='*' must be insertable");
  }

  @Test
  public void triggerRejectsUpdateOnSystemRole() {
    String pgCurrentUser = jooq.fetchOne("SELECT current_user").get(0, String.class);
    if ("admin".equals(pgCurrentUser)) {
      return;
    }
    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "UPDATE \"MOLGENIS\".role_permission_metadata"
                    + " SET select_scope = 'NONE'"
                    + " WHERE schema_name = ? AND role_name = 'Owner' AND table_name = '*'",
                SCHEMA_NAME),
        "UPDATE on system role row must be rejected by trigger");
  }

  @Test
  public void triggerAllowsCascadeDeleteWhenSchemaDropped() {
    String ephemeralSchemaForDelete = SCHEMA_NAME + "Del";
    db.dropSchemaIfExists(ephemeralSchemaForDelete);
    db.createSchema(ephemeralSchemaForDelete);

    List<Record> before =
        jooq.fetch(
            "SELECT role_name FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND table_name = '*'",
            ephemeralSchemaForDelete);
    assertFalse(before.isEmpty(), "system role rows must exist before drop");

    db.dropSchema(ephemeralSchemaForDelete);

    List<Record> after =
        jooq.fetch(
            "SELECT role_name FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ?",
            ephemeralSchemaForDelete);
    assertTrue(after.isEmpty(), "system role rows must cascade-delete when schema is dropped");
  }
}
