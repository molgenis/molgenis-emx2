package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlQuery.EXISTS_FIELD;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

class TestExistsField {

  private static final String SCHEMA_NAME = "EFTest";
  private static final String TABLE_NAME = "Obs";
  private static final String CUSTOM_ROLE = "researcher";
  private static final String TEST_USER = "EFTestUser";

  private static final SqlDatabase db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private static final SqlRoleManager roleManager = new SqlRoleManager(db);
  private static final ObjectMapper mapper = new ObjectMapper();

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME, column("id").setPkey()));
    schema.getTable(TABLE_NAME).insert(new Row().set("id", "r1"));
    if (!db.hasUser(TEST_USER)) db.addUser(TEST_USER);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void customRoleSelectExists_existsFieldPresent() throws Exception {
    grantCustomRole(SelectScope.EXISTS);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldPresent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectAll_existsFieldPresent() throws Exception {
    grantCustomRole(SelectScope.ALL);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldPresent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectOwn_existsFieldPresent() throws Exception {
    grantCustomRole(SelectScope.OWN);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldPresent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectGroup_existsFieldPresent() throws Exception {
    grantCustomRole(SelectScope.GROUP);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldPresent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectCount_existsFieldAbsent() throws Exception {
    grantCustomRole(SelectScope.COUNT);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldAbsent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectRange_existsFieldAbsent() throws Exception {
    grantCustomRole(SelectScope.RANGE);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldAbsent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectAggregate_existsFieldAbsent() throws Exception {
    grantCustomRole(SelectScope.AGGREGATE);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldAbsent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectNone_existsFieldAbsent() throws Exception {
    grantCustomRole(SelectScope.NONE);
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldAbsent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleExists_noCustomRole_existsFieldPresent() throws Exception {
    schema.addMember(TEST_USER, "Exists");
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldPresent();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleViewer_noCustomRole_existsFieldPresent() throws Exception {
    schema.addMember(TEST_USER, "Viewer");
    db.setActiveUser(TEST_USER);
    try {
      assertExistsFieldPresent();
    } finally {
      db.becomeAdmin();
    }
  }

  private void assertExistsFieldPresent() throws Exception {
    String json = schema.query(TABLE_NAME + "_agg", s(EXISTS_FIELD)).retrieveJSON();
    JsonNode root = mapper.readTree(json);
    JsonNode aggNode = root.get(TABLE_NAME + "_agg");
    assertNotNull(aggNode, "Aggregate node must be present");
    assertNotNull(aggNode.get(EXISTS_FIELD), "EXISTS field must be present in aggregate result");
  }

  private void assertExistsFieldAbsent() throws Exception {
    String json = schema.query(TABLE_NAME + "_agg", s(EXISTS_FIELD)).retrieveJSON();
    JsonNode root = mapper.readTree(json);
    JsonNode aggNode = root.get(TABLE_NAME + "_agg");
    assertTrue(
        aggNode == null || aggNode.get(EXISTS_FIELD) == null,
        "EXISTS field must be absent in aggregate result");
  }

  private void grantCustomRole(SelectScope scope) {
    db.becomeAdmin();
    if (!roleManager.listRoles(schema).isEmpty()) {
      try {
        roleManager.revokeRoleFromUser(schema, CUSTOM_ROLE, TEST_USER);
      } catch (Exception ignored) {
      }
      try {
        roleManager.deleteRole(schema, CUSTOM_ROLE);
      } catch (Exception ignored) {
      }
    }
    if (scope == SelectScope.OWN || scope == SelectScope.GROUP) {
      schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);
    }
    roleManager.createRole(schema, CUSTOM_ROLE, "test role");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).select(scope));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);
  }
}
