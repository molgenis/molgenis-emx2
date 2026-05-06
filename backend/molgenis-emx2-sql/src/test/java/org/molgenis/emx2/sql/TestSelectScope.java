package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

/**
 * Verifies COUNT-scoped role behaviour: RLS passes for COUNT scope (Path A, REQ-4) and
 * mg_privacy_count floors the result.
 */
class TestSelectScope {

  private static final String SCHEMA_NAME = "TestSelectScope";
  private static final String TABLE_NAME = "Observations";
  private static final String GROUP_ALPHA = "groupAlpha";
  private static final String ROLE_COUNT = "countRole";
  private static final String USER_ALICE = "TssAlice";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    roleManager.createGroup(schema, GROUP_ALPHA);
    roleManager.createRole(SCHEMA_NAME, ROLE_COUNT);

    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.COUNT);
    tp.setInsert(UpdateScope.NONE);
    tp.setUpdate(UpdateScope.NONE);
    tp.setDelete(UpdateScope.NONE);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_COUNT, ps);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ALPHA, USER_ALICE, ROLE_COUNT);

    for (int i = 0; i < 23; i++) {
      jooq.execute(
          "INSERT INTO \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" (id, val, mg_owner, mg_groups) VALUES (?, ?, ?, ?)",
          "row-" + i,
          "val-" + i,
          "MG_USER_admin",
          new String[] {GROUP_ALPHA});
    }
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void directSqlCountIsFloored() {
    db.setActiveUser(USER_ALICE);
    long rawCount;
    try {
      rawCount =
          jooq.fetchOne("SELECT COUNT(*) FROM \"" + SCHEMA_NAME + "\".\"" + TABLE_NAME + "\"")
              .get(0, Long.class);
    } finally {
      db.becomeAdmin();
    }
    assertEquals(
        23L, rawCount, "COUNT-scoped user must see raw count via RLS pass-through (Path A)");

    Long floored =
        jooq.fetchOne("SELECT \"MOLGENIS\".mg_privacy_count(?) AS cnt", rawCount)
            .get("cnt", Long.class);
    assertNotNull(floored, "mg_privacy_count must not return null");
    assertEquals(30L, floored, "mg_privacy_count(23) must ceil to 30");
    assertEquals(0L, floored % 10, "floored count must be a multiple of 10");
  }
}
