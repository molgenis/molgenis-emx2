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
  public void triggerRejectsUpdateOnSystemRole() {
    String triggerTestUser = "TriggerTestUser";
    if (!db.hasUser(triggerTestUser)) db.addUser(triggerTestUser);
    db.setActiveUser(triggerTestUser);
    try {
      assertThrows(
          Exception.class,
          () ->
              jooq.execute(
                  "UPDATE \"MOLGENIS\".role_permission_metadata"
                      + " SET select_scope = 'NONE'"
                      + " WHERE schema_name = ? AND role_name = 'Owner' AND table_name = '*'",
                  SCHEMA_NAME),
          "UPDATE on system role row must be rejected by trigger mg_protect_system_roles");
    } finally {
      db.becomeAdmin();
    }
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
