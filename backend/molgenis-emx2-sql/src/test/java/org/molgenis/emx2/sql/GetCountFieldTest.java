package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlQuery.COUNT_FIELD;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SelectScope;
import org.molgenis.emx2.TablePermission;

class GetCountFieldTest {

  private static final String SCHEMA_NAME = "GCFTest";
  private static final String TABLE_NAME = "Obs";
  private static final String CUSTOM_ROLE = "researcher";
  private static final String TEST_USER = "GCFTestUser";

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
    schema.getTable(TABLE_NAME).insert(new Row().set("id", "r2"));
    schema.getTable(TABLE_NAME).insert(new Row().set("id", "r3"));
    if (!db.hasUser(TEST_USER)) db.addUser(TEST_USER);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void customRoleSelectAll_returnsExactCount() throws Exception {
    grantCustomRole(SelectScope.ALL);
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(3, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectCount_returnsExactCount() throws Exception {
    grantCustomRole(SelectScope.COUNT);
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(3, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectAggregate_returnsExactCount() throws Exception {
    grantCustomRole(SelectScope.AGGREGATE);
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(3, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectRange_returnsPrivacyFloor() throws Exception {
    grantCustomRole(SelectScope.RANGE);
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(10, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectRange_aboveThreshold_appliesCeilFloor() throws Exception {
    db.becomeAdmin();
    for (int index = 4; index <= 11; index++) {
      schema.getTable(TABLE_NAME).insert(new Row().set("id", "r" + index));
    }
    grantCustomRole(SelectScope.RANGE);
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(20, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectExists_throws() {
    grantCustomRole(SelectScope.EXISTS);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, this::queryCount);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleSelectNone_throws() {
    grantCustomRole(SelectScope.NONE);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, this::queryCount);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleCount_noCustomRole_returnsExactCount() throws Exception {
    schema.addMember(TEST_USER, "Count");
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(3, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleRange_noCustomRole_returnsPrivacyFloor() throws Exception {
    schema.addMember(TEST_USER, "Range");
    db.setActiveUser(TEST_USER);
    try {
      int count = queryCount();
      assertEquals(10, count);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void privacyFloor_returnType_isLong() throws Exception {
    grantCustomRole(SelectScope.RANGE);
    db.setActiveUser(TEST_USER);
    try {
      String json = schema.query(TABLE_NAME + "_agg", s(COUNT_FIELD)).retrieveJSON();
      JsonNode root = mapper.readTree(json);
      JsonNode countNode = root.get(TABLE_NAME + "_agg").get(COUNT_FIELD);
      assertNotNull(countNode);
      assertTrue(countNode.isIntegralNumber(), "Privacy floor count must be integral (Long)");
      assertEquals(10L, countNode.longValue());
    } finally {
      db.becomeAdmin();
    }
  }

  private void grantCustomRole(SelectScope scope) {
    db.becomeAdmin();
    if (!roleManager.listRoles(schema).isEmpty()) {
      try {
        roleManager.revokeRoleFromUser(schema, CUSTOM_ROLE, TEST_USER);
      } catch (MolgenisException ignored) {
      }
      try {
        roleManager.deleteRole(schema, CUSTOM_ROLE);
      } catch (MolgenisException ignored) {
      }
    }
    schema.addMember(TEST_USER, "Exists");
    roleManager.createRole(schema, CUSTOM_ROLE, "test role");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).setSelect(scope));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);
  }

  private int queryCount() throws Exception {
    String json = schema.query(TABLE_NAME + "_agg", s(COUNT_FIELD)).retrieveJSON();
    JsonNode root = mapper.readTree(json);
    return root.get(TABLE_NAME + "_agg").get(COUNT_FIELD).intValue();
  }
}
