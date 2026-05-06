package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class TestSeedSystemRoles {

  private static final String SCHEMA_NAME = TestSeedSystemRoles.class.getSimpleName();
  private static Database db;
  private static DSLContext jooq;
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    schema = db.dropCreateSchema(SCHEMA_NAME);
  }

  @AfterAll
  public static void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  public void schemaCreateProducesFourSystemRoleRows() {
    List<Record> rows =
        jooq.fetch(
            "SELECT role_name, table_name FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND table_name = '*'"
                + " ORDER BY role_name",
            SCHEMA_NAME);
    assertEquals(4, rows.size(), "exactly four wildcard rows must exist after schema create");

    List<String> roleNames = rows.stream().map(r -> r.get("role_name", String.class)).toList();
    assertTrue(roleNames.contains("Owner"), "Owner row must exist");
    assertTrue(roleNames.contains("Manager"), "Manager row must exist");
    assertTrue(roleNames.contains("Editor"), "Editor row must exist");
    assertTrue(roleNames.contains("Viewer"), "Viewer row must exist");
  }

  @Test
  public void ownerRowHasCorrectScopes() {
    Record owner =
        jooq.fetchOne(
            "SELECT * FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND role_name = 'Owner' AND table_name = '*'",
            SCHEMA_NAME);
    assertNotNull(owner, "Owner wildcard row must exist");
    assertEquals("ALL", owner.get("select_scope", String.class));
    assertEquals("ALL", owner.get("insert_scope", String.class));
    assertEquals("ALL", owner.get("update_scope", String.class));
    assertEquals("ALL", owner.get("delete_scope", String.class));
    assertTrue(owner.get("change_owner", Boolean.class), "Owner.change_owner must be true");
    assertTrue(owner.get("change_group", Boolean.class), "Owner.change_group must be true");
  }

  @Test
  public void managerRowHasCorrectScopes() {
    Record manager =
        jooq.fetchOne(
            "SELECT * FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND role_name = 'Manager' AND table_name = '*'",
            SCHEMA_NAME);
    assertNotNull(manager, "Manager wildcard row must exist");
    assertEquals("ALL", manager.get("select_scope", String.class));
    assertEquals("ALL", manager.get("insert_scope", String.class));
    assertEquals("ALL", manager.get("update_scope", String.class));
    assertEquals("ALL", manager.get("delete_scope", String.class));
    assertTrue(manager.get("change_owner", Boolean.class), "Manager.change_owner must be true");
    assertTrue(manager.get("change_group", Boolean.class), "Manager.change_group must be true");
  }

  @Test
  public void editorRowHasCorrectScopes() {
    Record editor =
        jooq.fetchOne(
            "SELECT * FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND role_name = 'Editor' AND table_name = '*'",
            SCHEMA_NAME);
    assertNotNull(editor, "Editor wildcard row must exist");
    assertEquals("ALL", editor.get("select_scope", String.class));
    assertEquals("ALL", editor.get("insert_scope", String.class));
    assertEquals("ALL", editor.get("update_scope", String.class));
    assertEquals("ALL", editor.get("delete_scope", String.class));
    assertFalse(editor.get("change_owner", Boolean.class), "Editor.change_owner must be false");
    assertFalse(editor.get("change_group", Boolean.class), "Editor.change_group must be false");
  }

  @Test
  public void viewerRowHasCorrectScopes() {
    Record viewer =
        jooq.fetchOne(
            "SELECT * FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND role_name = 'Viewer' AND table_name = '*'",
            SCHEMA_NAME);
    assertNotNull(viewer, "Viewer wildcard row must exist");
    assertEquals("ALL", viewer.get("select_scope", String.class));
    assertEquals("NONE", viewer.get("insert_scope", String.class));
    assertEquals("NONE", viewer.get("update_scope", String.class));
    assertEquals("NONE", viewer.get("delete_scope", String.class));
    assertFalse(viewer.get("change_owner", Boolean.class), "Viewer.change_owner must be false");
    assertFalse(viewer.get("change_group", Boolean.class), "Viewer.change_group must be false");
  }

  @Test
  public void seedSystemRolesIsIdempotent() {
    MetadataUtils.seedSystemRoles(jooq, SCHEMA_NAME);
    MetadataUtils.seedSystemRoles(jooq, SCHEMA_NAME);

    List<Record> rows =
        jooq.fetch(
            "SELECT role_name FROM \"MOLGENIS\".role_permission_metadata"
                + " WHERE schema_name = ? AND table_name = '*'",
            SCHEMA_NAME);
    assertEquals(4, rows.size(), "repeated seedSystemRoles calls must not create duplicate rows");
  }
}
