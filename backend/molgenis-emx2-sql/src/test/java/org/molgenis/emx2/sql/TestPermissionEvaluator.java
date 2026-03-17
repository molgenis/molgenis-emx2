package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestPermissionEvaluator {

  private static Database database;
  private static final String SCHEMA = "TestPermissionEvaluator";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";
  private static final String ONTOLOGY_TABLE = "OntologyTable";

  private static final String USER_EXISTS = "pe_user_exists";
  private static final String USER_RANGE = "pe_user_range";
  private static final String USER_AGGREGATOR = "pe_user_aggregator";
  private static final String USER_COUNT = "pe_user_count";
  private static final String USER_VIEWER = "pe_user_viewer";
  private static final String USER_EDITOR = "pe_user_editor";
  private static final String USER_MANAGER = "pe_user_manager";
  private static final String USER_CUSTOM = "pe_user_custom";
  private static final String USER_NO_ROLE = "pe_user_norole";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user :
        List.of(
            USER_EXISTS,
            USER_RANGE,
            USER_AGGREGATOR,
            USER_COUNT,
            USER_VIEWER,
            USER_EDITOR,
            USER_MANAGER,
            USER_CUSTOM,
            USER_NO_ROLE)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()).add(column("value")),
        table(TABLE_B).add(column("id").setPkey()).add(column("value")),
        table(ONTOLOGY_TABLE).setTableType(TableType.ONTOLOGIES).add(column("name").setPkey()));

    // Assign system roles
    schema.addMember(USER_EXISTS, Privileges.EXISTS.toString());
    schema.addMember(USER_RANGE, Privileges.RANGE.toString());
    schema.addMember(USER_AGGREGATOR, Privileges.AGGREGATOR.toString());
    schema.addMember(USER_COUNT, Privileges.COUNT.toString());
    schema.addMember(USER_VIEWER, Privileges.VIEWER.toString());
    schema.addMember(USER_EDITOR, Privileges.EDITOR.toString());
    schema.addMember(USER_MANAGER, Privileges.MANAGER.toString());

    // Custom role with SELECT on TABLE_A only
    schema.createRole("CustomReader", "Can read TableA");
    schema.grant("CustomReader", new TablePermission(TABLE_A, true, null, null, null));
    schema.addMember(USER_CUSTOM, "CustomReader");
  }

  private PermissionEvaluator evaluatorFor(String user) {
    database.becomeAdmin();
    database.setActiveUser(user);
    return database.getSchema(SCHEMA).getPermissionEvaluator();
  }

  // --- canView ---

  @Test
  void viewerCanViewAllTables() {
    PermissionEvaluator eval = evaluatorFor(USER_VIEWER);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    TableMetadata tableB = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_B);
    assertTrue(eval.canView(tableA));
    assertTrue(eval.canView(tableB));
  }

  @Test
  void existsUserCannotViewTables() {
    PermissionEvaluator eval = evaluatorFor(USER_EXISTS);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertFalse(eval.canView(tableA));
  }

  @Test
  void customRoleCanViewGrantedTableOnly() {
    PermissionEvaluator eval = evaluatorFor(USER_CUSTOM);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    TableMetadata tableB = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_B);
    assertTrue(eval.canView(tableA));
    assertFalse(eval.canView(tableB));
  }

  @Test
  void ontologyTablesAlwaysViewable() {
    PermissionEvaluator eval = evaluatorFor(USER_EXISTS);
    TableMetadata ontology =
        database.getSchema(SCHEMA).getMetadata().getTableMetadata(ONTOLOGY_TABLE);
    assertTrue(eval.canView(ontology));
  }

  @Test
  void editorCanViewAllTables() {
    PermissionEvaluator eval = evaluatorFor(USER_EDITOR);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertTrue(eval.canView(tableA));
  }

  // --- getAggregateLevel ---

  @Test
  void viewerGetsFull() {
    PermissionEvaluator eval = evaluatorFor(USER_VIEWER);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertEquals(AggregateLevel.FULL, eval.getAggregateLevel(tableA));
  }

  @Test
  void countGetsCount() {
    PermissionEvaluator eval = evaluatorFor(USER_COUNT);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertEquals(AggregateLevel.COUNT, eval.getAggregateLevel(tableA));
  }

  @Test
  void aggregatorGetsAggregator() {
    PermissionEvaluator eval = evaluatorFor(USER_AGGREGATOR);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertEquals(AggregateLevel.AGGREGATOR, eval.getAggregateLevel(tableA));
  }

  @Test
  void rangeGetsRange() {
    PermissionEvaluator eval = evaluatorFor(USER_RANGE);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertEquals(AggregateLevel.RANGE, eval.getAggregateLevel(tableA));
  }

  @Test
  void existsGetsExists() {
    PermissionEvaluator eval = evaluatorFor(USER_EXISTS);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertEquals(AggregateLevel.EXISTS, eval.getAggregateLevel(tableA));
  }

  @Test
  void customRoleGetsFullOnGrantedTable() {
    PermissionEvaluator eval = evaluatorFor(USER_CUSTOM);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertEquals(AggregateLevel.FULL, eval.getAggregateLevel(tableA));
  }

  @Test
  void customRoleGetsExistsOnNonGrantedTable() {
    // Custom roles inherit EXISTS from createRole
    PermissionEvaluator eval = evaluatorFor(USER_CUSTOM);
    TableMetadata tableB = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_B);
    assertEquals(AggregateLevel.EXISTS, eval.getAggregateLevel(tableB));
  }

  @Test
  void ontologyTableGetsFull() {
    PermissionEvaluator eval = evaluatorFor(USER_EXISTS);
    TableMetadata ontology =
        database.getSchema(SCHEMA).getMetadata().getTableMetadata(ONTOLOGY_TABLE);
    assertEquals(AggregateLevel.FULL, eval.getAggregateLevel(ontology));
  }

  // --- canEdit ---

  @Test
  void editorCanEdit() {
    PermissionEvaluator eval = evaluatorFor(USER_EDITOR);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertTrue(eval.canEdit(tableA));
  }

  @Test
  void viewerCannotEdit() {
    PermissionEvaluator eval = evaluatorFor(USER_VIEWER);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertFalse(eval.canEdit(tableA));
  }

  @Test
  void customReaderCannotEdit() {
    PermissionEvaluator eval = evaluatorFor(USER_CUSTOM);
    TableMetadata tableA = database.getSchema(SCHEMA).getMetadata().getTableMetadata(TABLE_A);
    assertFalse(eval.canEdit(tableA));
  }

  @Test
  void customWriterCanEdit() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("CustomWriter", null);
    schema.grant("CustomWriter", new TablePermission(TABLE_A, true, true, true, null));
    schema.addMember(USER_NO_ROLE, "CustomWriter");

    try {
      PermissionEvaluator eval = evaluatorFor(USER_NO_ROLE);
      TableMetadata tableA = schema.getMetadata().getTableMetadata(TABLE_A);
      TableMetadata tableB = schema.getMetadata().getTableMetadata(TABLE_B);
      assertTrue(eval.canEdit(tableA));
      assertFalse(eval.canEdit(tableB));
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_NO_ROLE);
      schema.deleteRole("CustomWriter");
    }
  }

  // --- canManage ---

  @Test
  void managerCanManage() {
    PermissionEvaluator eval = evaluatorFor(USER_MANAGER);
    assertTrue(eval.canManage());
  }

  @Test
  void editorCannotManage() {
    PermissionEvaluator eval = evaluatorFor(USER_EDITOR);
    assertFalse(eval.canManage());
  }

  @Test
  void adminCanManage() {
    database.becomeAdmin();
    PermissionEvaluator eval = database.getSchema(SCHEMA).getPermissionEvaluator();
    assertTrue(eval.canManage());
  }

  // --- isAdmin ---

  @Test
  void adminIsAdmin() {
    database.becomeAdmin();
    PermissionEvaluator eval = database.getSchema(SCHEMA).getPermissionEvaluator();
    assertTrue(eval.isAdmin());
  }

  @Test
  void regularUserIsNotAdmin() {
    PermissionEvaluator eval = evaluatorFor(USER_VIEWER);
    assertFalse(eval.isAdmin());
  }
}
