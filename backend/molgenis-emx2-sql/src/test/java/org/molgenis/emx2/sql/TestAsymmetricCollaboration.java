package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

/**
 * Gate test for Phase 7: asymmetric collaboration where Alice is editor in group A and viewer in
 * group B within the same schema.
 */
public class TestAsymmetricCollaboration {

  private static final String SCHEMA_NAME = "TestAsymmetricCollab";
  private static final String TABLE_NAME = "Study";
  private static final String GROUP_A = "groupA";
  private static final String GROUP_B = "groupB";
  private static final String GROUP_C = "groupC";
  private static final String ROLE_EDITOR = "studyEditor";
  private static final String ROLE_VIEWER = "studyViewer";
  private static final String USER_ALICE = "AsymCollabAlice";

  private static final String ROW_A_ONLY = "row-a";
  private static final String ROW_B_ONLY = "row-b";
  private static final String ROW_A_B = "row-ab";

  private static Database db;
  private static DSLContext jooq;
  private static SqlRoleManager roleManager;
  private static Schema schema;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    roleManager = new SqlRoleManager((SqlDatabase) db);

    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);

    roleManager.createGroup(schema, GROUP_A);
    roleManager.createGroup(schema, GROUP_B);
    roleManager.createGroup(schema, GROUP_C);

    roleManager.createRole(SCHEMA_NAME, ROLE_EDITOR);
    roleManager.createRole(SCHEMA_NAME, ROLE_VIEWER);
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);

    PermissionSet editorPerms = new PermissionSet();
    editorPerms.setChangeGroup(true);
    TablePermission editorTp = new TablePermission(TABLE_NAME);
    editorTp.setSelect(SelectScope.GROUP);
    editorTp.setInsert(UpdateScope.GROUP);
    editorTp.setUpdate(UpdateScope.GROUP);
    editorTp.setDelete(UpdateScope.GROUP);
    editorPerms.putTable(TABLE_NAME, editorTp);
    roleManager.setPermissions(schema, ROLE_EDITOR, editorPerms);

    PermissionSet viewerPerms = new PermissionSet();
    TablePermission viewerTp = new TablePermission(TABLE_NAME);
    viewerTp.setSelect(SelectScope.GROUP);
    viewerTp.setInsert(UpdateScope.NONE);
    viewerTp.setUpdate(UpdateScope.NONE);
    viewerTp.setDelete(UpdateScope.NONE);
    viewerPerms.putTable(TABLE_NAME, viewerTp);
    roleManager.setPermissions(schema, ROLE_VIEWER, viewerPerms);

    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, ROLE_EDITOR);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_B, USER_ALICE, ROLE_VIEWER);

    insertRow(ROW_A_ONLY, "val-a", new String[] {GROUP_A});
    insertRow(ROW_B_ONLY, "val-b", new String[] {GROUP_B});
    insertRow(ROW_A_B, "val-ab", new String[] {GROUP_A, GROUP_B});
  }

  @AfterAll
  static void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private static void insertRow(String id, String val, String[] groups) {
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_NAME
            + "\".\""
            + TABLE_NAME
            + "\" (id, val, mg_owner, mg_groups) VALUES (?, ?, ?, ?)",
        id,
        val,
        "admin",
        groups);
  }

  private List<Row> selectAsAlice() {
    db.setActiveUser(USER_ALICE);
    try {
      return db.getSchema(SCHEMA_NAME).getTable(TABLE_NAME).retrieveRows();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void readsRowsFromBothGroups() {
    List<Row> rows = selectAsAlice();
    List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
    assertTrue(ids.contains(ROW_A_ONLY), "Alice must see row tagged [A] via editor membership");
    assertTrue(ids.contains(ROW_B_ONLY), "Alice must see row tagged [B] via viewer membership");
    assertTrue(ids.contains(ROW_A_B), "Alice must see row tagged [A,B]");
  }

  @Test
  void updatesAGroupRow() {
    db.setActiveUser(USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              db.getSchema(SCHEMA_NAME)
                  .getTable(TABLE_NAME)
                  .update(new Row().setString("id", ROW_A_ONLY).setString("val", "updated-a")),
          "Alice must be able to UPDATE row tagged [A] — editor authority via group A");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void rejectsUpdateOfBOnlyRow() {
    db.setActiveUser(USER_ALICE);
    try {
      int affected =
          db.getSchema(SCHEMA_NAME)
              .getTable(TABLE_NAME)
              .update(new Row().setString("id", ROW_B_ONLY).setString("val", "should-fail"));
      assertEquals(
          0, affected, "Alice must not be able to UPDATE row tagged [B] — viewer-only in B");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void allowsRemovingGroupSheHasNoAuthorityIn() {
    db.setActiveUser(USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" SET mg_groups = ARRAY[?]::text[] WHERE id = ?",
                  GROUP_A,
                  ROW_A_B),
          "Alice can pull row tagged [A,B] down to [A] because GROUP_A is within her editor authority");
    } finally {
      db.becomeAdmin();
      jooq.execute(
          "UPDATE \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" SET mg_groups = ARRAY[?,?]::text[] WHERE id = ?",
          GROUP_A,
          GROUP_B,
          ROW_A_B);
    }
  }

  @Test
  void rejectsShareIntoForeignGroup() {
    db.setActiveUser(USER_ALICE);
    try {
      assertThrows(
          Exception.class,
          () ->
              jooq.execute(
                  "UPDATE \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" SET mg_groups = ARRAY[?,?]::text[] WHERE id = ?",
                  GROUP_A,
                  GROUP_C,
                  ROW_A_ONLY),
          "Alice must not be able to share row into group C where she has no membership");
    } finally {
      db.becomeAdmin();
    }
  }
}
