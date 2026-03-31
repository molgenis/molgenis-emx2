package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
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

    schema.addMember(USER_EXISTS, Privileges.EXISTS.toString());
    schema.addMember(USER_RANGE, Privileges.RANGE.toString());
    schema.addMember(USER_AGGREGATOR, Privileges.AGGREGATOR.toString());
    schema.addMember(USER_COUNT, Privileges.COUNT.toString());
    schema.addMember(USER_VIEWER, Privileges.VIEWER.toString());
    schema.addMember(USER_EDITOR, Privileges.EDITOR.toString());
    schema.addMember(USER_MANAGER, Privileges.MANAGER.toString());

    schema.createRole("CustomReader");
    schema.grant("CustomReader", new TablePermission(TABLE_A).select(true));
    schema.addMember(USER_CUSTOM, "CustomReader");
  }

  private static Schema schemaFor(String user) {
    database.becomeAdmin();
    database.setActiveUser(user);
    return database.getSchema(SCHEMA);
  }

  @Nested
  class CanView {

    @Test
    void viewerCanViewAllTables() {
      Schema s = schemaFor(USER_VIEWER);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      TableMetadata tableB = s.getMetadata().getTableMetadata(TABLE_B);
      assertTrue(PermissionEvaluator.canView(s, tableA));
      assertTrue(PermissionEvaluator.canView(s, tableB));
    }

    @Test
    void existsUserCannotViewTables() {
      Schema s = schemaFor(USER_EXISTS);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertFalse(PermissionEvaluator.canView(s, tableA));
    }

    @Test
    void customRoleCanViewGrantedTableOnly() {
      Schema s = schemaFor(USER_CUSTOM);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      TableMetadata tableB = s.getMetadata().getTableMetadata(TABLE_B);
      assertTrue(PermissionEvaluator.canView(s, tableA));
      assertFalse(PermissionEvaluator.canView(s, tableB));
    }

    @Test
    void ontologyTablesAlwaysViewable() {
      Schema s = schemaFor(USER_EXISTS);
      TableMetadata ontology = s.getMetadata().getTableMetadata(ONTOLOGY_TABLE);
      assertTrue(PermissionEvaluator.canView(s, ontology));
    }

    @Test
    void editorCanViewAllTables() {
      Schema s = schemaFor(USER_EDITOR);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertTrue(PermissionEvaluator.canView(s, tableA));
    }
  }

  @Nested
  class GetAggregateLevel {

    @Test
    void viewerGetsCount() {
      Schema s = schemaFor(USER_VIEWER);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertEquals(AggregateLevel.COUNT, PermissionEvaluator.getAggregateLevel(s, tableA));
    }

    @Test
    void countGetsCount() {
      Schema s = schemaFor(USER_COUNT);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertEquals(AggregateLevel.COUNT, PermissionEvaluator.getAggregateLevel(s, tableA));
    }

    @Test
    void aggregatorGetsAggregator() {
      Schema s = schemaFor(USER_AGGREGATOR);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertEquals(AggregateLevel.AGGREGATOR, PermissionEvaluator.getAggregateLevel(s, tableA));
    }

    @Test
    void rangeGetsRange() {
      Schema s = schemaFor(USER_RANGE);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertEquals(AggregateLevel.RANGE, PermissionEvaluator.getAggregateLevel(s, tableA));
    }

    @Test
    void existsGetsExists() {
      Schema s = schemaFor(USER_EXISTS);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertEquals(AggregateLevel.EXISTS, PermissionEvaluator.getAggregateLevel(s, tableA));
    }

    @Test
    void customRoleGetsCountOnGrantedTable() {
      Schema s = schemaFor(USER_CUSTOM);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertEquals(AggregateLevel.COUNT, PermissionEvaluator.getAggregateLevel(s, tableA));
    }

    @Test
    void customRoleGetsExistsOnNonGrantedTable() {
      Schema s = schemaFor(USER_CUSTOM);
      TableMetadata tableB = s.getMetadata().getTableMetadata(TABLE_B);
      assertEquals(AggregateLevel.EXISTS, PermissionEvaluator.getAggregateLevel(s, tableB));
    }

    @Test
    void ontologyTableGetsCount() {
      Schema s = schemaFor(USER_EXISTS);
      TableMetadata ontology = s.getMetadata().getTableMetadata(ONTOLOGY_TABLE);
      assertEquals(AggregateLevel.COUNT, PermissionEvaluator.getAggregateLevel(s, ontology));
    }
  }

  @Nested
  class CanInsert {

    @Test
    void editorCanInsert() {
      Schema s = schemaFor(USER_EDITOR);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertTrue(PermissionEvaluator.canInsert(s, tableA));
    }

    @Test
    void viewerCannotInsert() {
      Schema s = schemaFor(USER_VIEWER);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertFalse(PermissionEvaluator.canInsert(s, tableA));
    }

    @Test
    void customReaderCannotInsert() {
      Schema s = schemaFor(USER_CUSTOM);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertFalse(PermissionEvaluator.canInsert(s, tableA));
    }

    @Test
    void customWriterCanInsertGrantedTableOnly() {
      database.becomeAdmin();
      Schema schema = database.getSchema(SCHEMA);
      schema.createRole("InsertWriter");
      schema.grant("InsertWriter", new TablePermission(TABLE_A).select(true).insert(true));
      schema.addMember(USER_NO_ROLE, "InsertWriter");

      try {
        Schema s = schemaFor(USER_NO_ROLE);
        TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
        TableMetadata tableB = s.getMetadata().getTableMetadata(TABLE_B);
        assertTrue(PermissionEvaluator.canInsert(s, tableA));
        assertFalse(PermissionEvaluator.canInsert(s, tableB));
      } finally {
        database.becomeAdmin();
        schema.removeMember(USER_NO_ROLE);
        schema.deleteRole("InsertWriter");
      }
    }
  }

  @Nested
  class CanUpdate {

    @Test
    void editorCanUpdate() {
      Schema s = schemaFor(USER_EDITOR);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertTrue(PermissionEvaluator.canUpdate(s, tableA));
    }

    @Test
    void viewerCannotUpdate() {
      Schema s = schemaFor(USER_VIEWER);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertFalse(PermissionEvaluator.canUpdate(s, tableA));
    }

    @Test
    void customWriterCanUpdateGrantedTableOnly() {
      database.becomeAdmin();
      Schema schema = database.getSchema(SCHEMA);
      schema.createRole("UpdateWriter");
      schema.grant("UpdateWriter", new TablePermission(TABLE_A).select(true).update(true));
      schema.addMember(USER_NO_ROLE, "UpdateWriter");

      try {
        Schema s = schemaFor(USER_NO_ROLE);
        TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
        TableMetadata tableB = s.getMetadata().getTableMetadata(TABLE_B);
        assertTrue(PermissionEvaluator.canUpdate(s, tableA));
        assertFalse(PermissionEvaluator.canUpdate(s, tableB));
      } finally {
        database.becomeAdmin();
        schema.removeMember(USER_NO_ROLE);
        schema.deleteRole("UpdateWriter");
      }
    }

    @Test
    void insertOnlyGrantCannotUpdate() {
      database.becomeAdmin();
      Schema schema = database.getSchema(SCHEMA);
      schema.createRole("InsertOnly");
      schema.grant("InsertOnly", new TablePermission(TABLE_A).select(true).insert(true));
      schema.addMember(USER_NO_ROLE, "InsertOnly");

      try {
        Schema s = schemaFor(USER_NO_ROLE);
        TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
        assertFalse(PermissionEvaluator.canUpdate(s, tableA));
      } finally {
        database.becomeAdmin();
        schema.removeMember(USER_NO_ROLE);
        schema.deleteRole("InsertOnly");
      }
    }
  }

  @Nested
  class CanDelete {

    @Test
    void editorCanDelete() {
      Schema s = schemaFor(USER_EDITOR);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertTrue(PermissionEvaluator.canDelete(s, tableA));
    }

    @Test
    void viewerCannotDelete() {
      Schema s = schemaFor(USER_VIEWER);
      TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
      assertFalse(PermissionEvaluator.canDelete(s, tableA));
    }

    @Test
    void customWriterCanDeleteGrantedTableOnly() {
      database.becomeAdmin();
      Schema schema = database.getSchema(SCHEMA);
      schema.createRole("DeleteWriter");
      schema.grant("DeleteWriter", new TablePermission(TABLE_A).select(true).delete(true));
      schema.addMember(USER_NO_ROLE, "DeleteWriter");

      try {
        Schema s = schemaFor(USER_NO_ROLE);
        TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
        TableMetadata tableB = s.getMetadata().getTableMetadata(TABLE_B);
        assertTrue(PermissionEvaluator.canDelete(s, tableA));
        assertFalse(PermissionEvaluator.canDelete(s, tableB));
      } finally {
        database.becomeAdmin();
        schema.removeMember(USER_NO_ROLE);
        schema.deleteRole("DeleteWriter");
      }
    }

    @Test
    void insertUpdateGrantCannotDelete() {
      database.becomeAdmin();
      Schema schema = database.getSchema(SCHEMA);
      schema.createRole("NoDelete");
      schema.grant("NoDelete", new TablePermission(TABLE_A).select(true).insert(true).update(true));
      schema.addMember(USER_NO_ROLE, "NoDelete");

      try {
        Schema s = schemaFor(USER_NO_ROLE);
        TableMetadata tableA = s.getMetadata().getTableMetadata(TABLE_A);
        assertFalse(PermissionEvaluator.canDelete(s, tableA));
      } finally {
        database.becomeAdmin();
        schema.removeMember(USER_NO_ROLE);
        schema.deleteRole("NoDelete");
      }
    }
  }

  @Nested
  class CanManage {

    @Test
    void managerCanManage() {
      Schema s = schemaFor(USER_MANAGER);
      assertTrue(PermissionEvaluator.canManage(s));
    }

    @Test
    void editorCannotManage() {
      Schema s = schemaFor(USER_EDITOR);
      assertFalse(PermissionEvaluator.canManage(s));
    }

    @Test
    void adminCanManage() {
      database.becomeAdmin();
      Schema s = database.getSchema(SCHEMA);
      assertTrue(PermissionEvaluator.canManage(s));
    }
  }

  @Nested
  class IsAdmin {

    @Test
    void adminIsAdmin() {
      database.becomeAdmin();
      Schema s = database.getSchema(SCHEMA);
      assertTrue(PermissionEvaluator.isAdmin(s));
    }

    @Test
    void regularUserIsNotAdmin() {
      Schema s = schemaFor(USER_VIEWER);
      assertFalse(PermissionEvaluator.isAdmin(s));
    }
  }
}
