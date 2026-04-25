package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;

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
    schema.grant(
        "ViewerRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
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
    schema.grant(
        "PartialRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(USER_VIEWER, "PartialRole");

    database.setActiveUser(USER_VIEWER);

    // TABLE_A: should succeed
    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
    // TABLE_B: should fail – no grant
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
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.ALL,
            UpdateScope.ALL,
            UpdateScope.ALL,
            false,
            false));
    schema.addMember(USER_EDITOR, "EditorRole");

    database.setActiveUser(USER_EDITOR);

    database
        .getSchema(SCHEMA)
        .getTable(TABLE_A)
        .insert(new Row().setString("id", "r_editor").setString("value", "inserted"));

    database.becomeAdmin();
    List<Row> rows = schema.getTable(TABLE_A).retrieveRows();
    assertTrue(rows.stream().anyMatch(r -> "r_editor".equals(r.getString("id"))));

    // Cleanup
    schema.getTable(TABLE_A).delete(new Row().setString("id", "r_editor"));
  }

  @Test
  void userWithoutWritePermissionCannotInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ReadOnlyRole");
    schema.grant(
        "ReadOnlyRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
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
    // Ontology tables should be accessible regardless of grants
    List<Row> rows = database.getSchema(SCHEMA).getTable(ONTOLOGY_TABLE).retrieveRows();
    assertNotNull(rows);
  }

  @Test
  void userWithSelectPermissionCanSeeCount() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("CountRole");
    schema.grant(
        "CountRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(USER_VIEWER, "CountRole");

    database.setActiveUser(USER_VIEWER);
    // A user with table-level SELECT (but no VIEWER/COUNT role) should be able to query
    List<Row> rows = database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows();
    assertFalse(rows.isEmpty(), "User with SELECT permission should see rows and thus count > 0");
  }
}
