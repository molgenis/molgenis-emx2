package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

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
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);

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
  void countScopeRlsPassThroughSeesAllRows() {
    db.setActiveUser(USER_ALICE);
    List<Row> rows;
    try {
      rows = schema.getTable(TABLE_NAME).retrieveRows();
    } finally {
      db.becomeAdmin();
    }
    assertEquals(
        23,
        rows.size(),
        "COUNT-scoped user must see all rows via RLS pass-through (Path A, REQ-4)");
  }
}
