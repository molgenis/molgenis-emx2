package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

/** Tests that table-level permissions are actually enforced at the SQL layer. */
class TestTablePermissionEnforcement {

  private static Database database;
  private static final String SCHEMA = "TestTablePermEnforce";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";
  private static final String ONTOLOGY_TABLE = "OntologyTable";

  private static final String USER_VIEWER = "tpe_user_viewer";
  private static final String USER_EDITOR = "tpe_user_editor";
  private static final String USER_NO_ACCESS = "tpe_user_noaccess";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user : List.of(USER_VIEWER, USER_EDITOR, USER_NO_ACCESS)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()).add(column("value")),
        table(TABLE_B).add(column("id").setPkey()).add(column("value")),
        table(ONTOLOGY_TABLE).setTableType(TableType.ONTOLOGIES));

    schema.getTable(TABLE_A).insert(new Row().setString("id", "r1").setString("value", "hello"));
    schema.getTable(TABLE_B).insert(new Row().setString("id", "r1").setString("value", "world"));
    schema.getTable(ONTOLOGY_TABLE).insert(new Row().setString("name", "term1").setInt("order", 1));
  }

  @Test
  void userWithViewerRoleCanSelectGrantedTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ViewerRole");
    schema.grant("ViewerRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.addMember(USER_VIEWER, "ViewerRole");

    database.setActiveUser(USER_VIEWER);

    List<Row> rows = database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows();
    assertEquals(1, rows.size());
  }

  @Test
  void userWithoutGrantCannotSelectTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("NoTableRole");
    schema.addMember(USER_NO_ACCESS, "NoTableRole");

    database.setActiveUser(USER_NO_ACCESS);
    assertThrows(
        Exception.class, () -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
  }

  @Test
  void userCanOnlySeeGrantedTables() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("PartialRole");
    schema.grant("PartialRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.addMember(USER_VIEWER, "PartialRole");

    database.setActiveUser(USER_VIEWER);

    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
    assertThrows(
        Exception.class, () -> database.getSchema(SCHEMA).getTable(TABLE_B).retrieveRows());
  }

  @Test
  void userWithInsertPermissionCanInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("EditorRole");
    schema.grant(
        "EditorRole",
        new TablePermission(TABLE_A)
            .select(SelectScope.ALL)
            .insert(UpdateScope.ALL)
            .update(UpdateScope.ALL)
            .delete(UpdateScope.ALL));
    schema.addMember(USER_EDITOR, "EditorRole");

    database.setActiveUser(USER_EDITOR);

    database
        .getSchema(SCHEMA)
        .getTable(TABLE_A)
        .insert(new Row().setString("id", "r_editor").setString("value", "inserted"));

    database.becomeAdmin();
    List<Row> rows = schema.getTable(TABLE_A).retrieveRows();
    assertTrue(rows.stream().anyMatch(r -> "r_editor".equals(r.getString("id"))));

    schema.getTable(TABLE_A).delete(new Row().setString("id", "r_editor"));
  }

  @Test
  void userWithoutWritePermissionCannotInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ReadOnlyRole");
    schema.grant("ReadOnlyRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.addMember(USER_EDITOR, "ReadOnlyRole");

    database.setActiveUser(USER_EDITOR);
    assertThrows(
        Exception.class,
        () ->
            database
                .getSchema(SCHEMA)
                .getTable(TABLE_A)
                .insert(new Row().setString("id", "fail").setString("value", "x")));
  }

  @Test
  void ontologyTableVisibleToUserWithNoGrants() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("EmptyRole");
    schema.addMember(USER_NO_ACCESS, "EmptyRole");

    database.setActiveUser(USER_NO_ACCESS);
    List<Row> rows = database.getSchema(SCHEMA).getTable(ONTOLOGY_TABLE).retrieveRows();
    assertNotNull(rows);
  }

  @Test
  void userWithSelectPermissionCanSeeCount() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("CountRole");
    schema.grant("CountRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.addMember(USER_VIEWER, "CountRole");

    database.setActiveUser(USER_VIEWER);
    List<Row> rows = database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows();
    assertFalse(rows.isEmpty(), "User with SELECT permission should see rows and thus count > 0");
  }
}
