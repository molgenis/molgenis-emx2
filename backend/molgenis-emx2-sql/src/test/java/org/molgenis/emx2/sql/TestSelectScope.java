package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
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
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    roleManager.createGroup(schema, GROUP_ALPHA);
    roleManager.createRole(schema, ROLE_COUNT, "");
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);

    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(SelectScope.COUNT);
    tp.insert(UpdateScope.NONE);
    tp.update(UpdateScope.NONE);
    tp.delete(UpdateScope.NONE);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_COUNT, ps);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ALPHA, USER_ALICE, ROLE_COUNT);

    Table observationsTable = schema.getTable(TABLE_NAME);
    for (int i = 0; i < 23; i++) {
      observationsTable.insert(
          new Row()
              .setString("id", "row-" + i)
              .setString("val", "val-" + i)
              .setStringArray("mg_groups", GROUP_ALPHA));
    }
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
