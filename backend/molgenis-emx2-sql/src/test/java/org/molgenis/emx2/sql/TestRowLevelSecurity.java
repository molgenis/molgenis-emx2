package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_GROUP;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;

@Tag("rowlevel")
public class TestRowLevelSecurity {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  private DSLContext jooq(Database db) {
    return ((SqlDatabase) db).getJooq();
  }

  @Test
  public void testEnableRlsAddsMgGroupColumn() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_enableRls");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          Boolean columnExists =
              jooq(db)
                  .fetchOne(
                      "SELECT EXISTS(SELECT 1 FROM information_schema.columns "
                          + "WHERE table_schema = {0} AND table_name = {1} AND column_name = {2})",
                      schema.getName(), "datatable", MG_GROUP)
                  .into(Boolean.class);
          assertTrue(columnExists, "mg_group column should exist after enabling RLS");

          Boolean rlsEnabled =
              jooq(db)
                  .fetchOne(
                      "SELECT relrowsecurity FROM pg_class "
                          + "WHERE relname = {0} AND relnamespace = "
                          + "(SELECT oid FROM pg_namespace WHERE nspname = {1})",
                      "datatable", schema.getName())
                  .into(Boolean.class);
          assertTrue(rlsEnabled, "RLS should be enabled on table");
        });
  }

  @Test
  public void testRowLevelUserSeesOnlyOwnGroupRows() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_ownGroup");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);
          rm.createRole(schema.getName(), "GroupB", true);

          db.addUser("rls_user1");
          rm.addMember(schema.getName(), "GroupA", "rls_user1");

          table.insert(
              new Row()
                  .setString("id", "1")
                  .setString("data", "data1")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}),
              new Row()
                  .setString("id", "2")
                  .setString("data", "data2")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupB")}),
              new Row()
                  .setString("id", "3")
                  .setString("data", "data3")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}));

          db.setActiveUser("rls_user1");

          List<Row> rows = schema.getTable("DataTable").retrieveRows();
          assertEquals(2, rows.size(), "Row-level user should see only GroupA rows");
        });
  }

  @Test
  public void testSchemaLevelUserSeesAllRows() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_schemaLevel");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);
          rm.createRole(schema.getName(), "GroupB", true);

          db.addUser("rls_viewer");
          schema.addMember("rls_viewer", VIEWER.toString());

          table.insert(
              new Row()
                  .setString("id", "1")
                  .setString("data", "data1")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}),
              new Row()
                  .setString("id", "2")
                  .setString("data", "data2")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupB")}));

          db.setActiveUser("rls_viewer");

          List<Row> rows = schema.getTable("DataTable").retrieveRows();
          assertEquals(2, rows.size(), "Schema-level Viewer should see all rows");
        });
  }

  @Test
  public void testUserInBothRowLevelAndSchemaLevel() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_bothLevels");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);

          rm.createRole(schema.getName(), "GroupB", true);

          db.addUser("rls_both");
          rm.addMember(schema.getName(), "GroupA", "rls_both");
          schema.addMember("rls_both", VIEWER.toString());

          table.insert(
              new Row()
                  .setString("id", "1")
                  .setString("data", "data1")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}),
              new Row()
                  .setString("id", "2")
                  .setString("data", "data2")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupB")}));

          db.setActiveUser("rls_both");

          List<Row> rows = schema.getTable("DataTable").retrieveRows();
          assertEquals(
              2,
              rows.size(),
              "User with both row-level and schema-level roles should see all rows");
        });
  }

  @Test
  public void testNullMgGroupVisibleToAll() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_nullGroup");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);

          db.addUser("rls_null_user");
          rm.addMember(schema.getName(), "GroupA", "rls_null_user");

          table.insert(
              new Row().setString("id", "1").setString("data", "public"),
              new Row()
                  .setString("id", "2")
                  .setString("data", "private")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}));

          db.setActiveUser("rls_null_user");

          List<Row> rows = schema.getTable("DataTable").retrieveRows();
          assertTrue(
              rows.stream().anyMatch(r -> "1".equals(r.getString("id"))),
              "Row-level user should see rows with NULL mg_group");
        });
  }

  @Test
  public void testDisableRlsRemovesPolicies() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_disableRls");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          Boolean rlsBefore =
              jooq(db)
                  .fetchOne(
                      "SELECT relrowsecurity FROM pg_class "
                          + "WHERE relname = {0} AND relnamespace = "
                          + "(SELECT oid FROM pg_namespace WHERE nspname = {1})",
                      "datatable", schema.getName())
                  .into(Boolean.class);
          assertTrue(rlsBefore, "RLS should be enabled before disable");

          table.getMetadata().disableRowLevelSecurity();

          Boolean columnStillExists =
              jooq(db)
                  .fetchOne(
                      "SELECT EXISTS(SELECT 1 FROM information_schema.columns "
                          + "WHERE table_schema = {0} AND table_name = {1} AND column_name = {2})",
                      schema.getName(), "datatable", MG_GROUP)
                  .into(Boolean.class);
          assertTrue(columnStillExists, "mg_group column should be preserved after disabling RLS");

          Boolean rlsAfter =
              jooq(db)
                  .fetchOne(
                      "SELECT relrowsecurity FROM pg_class "
                          + "WHERE relname = {0} AND relnamespace = "
                          + "(SELECT oid FROM pg_namespace WHERE nspname = {1})",
                      "datatable", schema.getName())
                  .into(Boolean.class);
          assertFalse(rlsAfter, "RLS should be disabled after disable");
        });
  }

  @Test
  public void testDeleteWithRls() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_delete");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);
          rm.createRole(schema.getName(), "GroupB", true);

          db.addUser("rls_deleter");
          rm.addMember(schema.getName(), "GroupA", "rls_deleter");

          String groupA = SqlRoleManager.fullRoleName(schema.getName(), "GroupA");
          String groupB = SqlRoleManager.fullRoleName(schema.getName(), "GroupB");

          table.insert(
              new Row()
                  .setString("id", "1")
                  .setString("data", "data1")
                  .set(MG_GROUP, new String[] {groupA}),
              new Row()
                  .setString("id", "2")
                  .setString("data", "data2")
                  .set(MG_GROUP, new String[] {groupB}));

          db.setActiveUser("rls_deleter");
          table.delete(new Row().setString("id", "1"));

          db.becomeAdmin();
          List<Row> rows = schema.getTable("DataTable").retrieveRows();
          assertEquals(1, rows.size(), "Only GroupB row should remain");
          assertEquals("2", rows.get(0).getString("id"));
        });
  }

  @Test
  public void testAdminBypassesRls() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_adminBypass");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);
          rm.createRole(schema.getName(), "GroupB", true);

          db.addUser("rls_admin");
          schema.addMember("rls_admin", OWNER.toString());

          table.insert(
              new Row()
                  .setString("id", "1")
                  .setString("data", "data1")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}),
              new Row()
                  .setString("id", "2")
                  .setString("data", "data2")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupB")}));

          db.setActiveUser("rls_admin");

          List<Row> rows = schema.getTable("DataTable").retrieveRows();
          assertEquals(2, rows.size(), "Owner should see all rows bypassing RLS");
        });
  }

  @Test
  public void testAnonymousUserSeesNothing() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRLS_anonymous");
          Table table =
              schema.create(table("DataTable").add(column("id").setPkey()).add(column("data")));

          table.getMetadata().enableRowLevelSecurity();

          SqlRoleManager rm = ((SqlDatabase) db).getRoleManager();
          rm.createRole(schema.getName(), "GroupA", true);

          table.insert(
              new Row()
                  .setString("id", "1")
                  .setString("data", "data1")
                  .set(
                      MG_GROUP,
                      new String[] {SqlRoleManager.fullRoleName(schema.getName(), "GroupA")}));

          db.setActiveUser("anonymous");

          assertThrows(
              MolgenisException.class,
              () -> schema.getTable("DataTable").retrieveRows(),
              "Anonymous user should not see any rows");
        });
  }
}
