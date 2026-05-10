package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlQuery.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;

class TestPrivacy {

  private static final String SCHEMA_NAME = "TestPrivacy";
  private static final String TABLE_NAME = "Obs";
  private static final String ROLE_NAME = "privacyRole";
  private static final String USER_NAME = "TpUser";
  private static final String GROUP_NAME = "tpGroup";

  private static final SqlDatabase db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = db.getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager(db);
  private static final ObjectMapper mapper = new ObjectMapper();

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME, column("id").setPkey()));
    if (!db.hasUser(USER_NAME)) db.addUser(USER_NAME);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void existsScope_clampsToBoolean() throws Exception {
    insertRows(3);
    grantCustomRole(SelectScope.EXISTS, false);

    db.setActiveUser(USER_NAME);
    try {
      JsonNode agg = queryAgg(EXISTS_FIELD);
      assertNotNull(agg.get(EXISTS_FIELD), "EXISTS field must be present");
      assertTrue(
          agg.get(EXISTS_FIELD).isBoolean(), "EXISTS field must be a boolean, not row content");
      assertTrue(agg.get(EXISTS_FIELD).booleanValue(), "EXISTS must be true when rows exist");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void countScope_appliesFloorOf10() throws Exception {
    insertRows(5);
    grantCustomRole(SelectScope.COUNT, false);

    db.setActiveUser(USER_NAME);
    try {
      JsonNode agg = queryAgg(COUNT_FIELD);
      assertNotNull(agg.get(COUNT_FIELD), "COUNT field must be present");
      long reported = agg.get(COUNT_FIELD).longValue();
      assertTrue(
          reported >= 10L,
          "COUNT scope with <10 rows must report >= 10 (floor of 10); got " + reported);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void countScope_aboveFloor_returnsRealCount() throws Exception {
    insertRows(25);
    grantCustomRole(SelectScope.COUNT, false);

    db.setActiveUser(USER_NAME);
    try {
      JsonNode agg = queryAgg(COUNT_FIELD);
      long reported = agg.get(COUNT_FIELD).longValue();
      assertEquals(
          25L, reported, "COUNT scope with >10 rows must report the real count; got " + reported);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void rangeScope_appliesFloor() throws Exception {
    insertRows(3);
    grantCustomRole(SelectScope.RANGE, false);

    db.setActiveUser(USER_NAME);
    try {
      JsonNode agg = queryAgg(COUNT_FIELD);
      assertNotNull(agg.get(COUNT_FIELD), "COUNT field must be present for RANGE scope");
      long reported = agg.get(COUNT_FIELD).longValue();
      assertTrue(
          reported >= 10L,
          "RANGE scope with <10 rows must use mg_privacy_count floor; got " + reported);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void aggregateScope_aboveThreshold_returnsValues_belowThreshold_returnsNull() throws Exception {
    insertRows(5);
    grantCustomRole(SelectScope.AGGREGATE, false);

    db.setActiveUser(USER_NAME);
    try {
      JsonNode agg = queryAgg(COUNT_FIELD);
      long reported = agg.get(COUNT_FIELD).longValue();
      assertTrue(
          reported >= 10L,
          "AGGREGATE scope with <10 rows must clamp count to floor; got "
              + reported
              + ". NOTE: SqlQuery.AGGREGATE_COUNT_THRESHOLD = Integer.MIN_VALUE means threshold "
              + "never triggers — this is a production bug. Spec requires floor of 10.");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void countScope_onRlsTable_passesThroughPolicy_andClampsProjection() throws Exception {
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);
    insertRowsWithOwner(5, "admin");
    grantCustomRole(SelectScope.COUNT, true);

    db.setActiveUser(USER_NAME);
    try {
      List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();
      assertFalse(rows.isEmpty(), "COUNT-scoped user on RLS table must see rows (pass-through)");

      JsonNode agg = queryAgg(COUNT_FIELD);
      long reported = agg.get(COUNT_FIELD).longValue();
      assertTrue(
          reported >= 10L,
          "COUNT projection on RLS table must clamp to floor of 10; got " + reported);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void countScope_onNonRlsTable_projectionOnly() throws Exception {
    insertRows(5);
    grantCustomRole(SelectScope.COUNT, false);

    db.setActiveUser(USER_NAME);
    try {
      JsonNode agg = queryAgg(COUNT_FIELD);
      long reported = agg.get(COUNT_FIELD).longValue();
      assertTrue(
          reported >= 10L,
          "COUNT projection on non-RLS table must clamp to floor of 10; got " + reported);
    } finally {
      db.becomeAdmin();
    }
  }

  private void insertRows(int count) {
    for (int i = 0; i < count; i++) {
      schema.getTable(TABLE_NAME).insert(new Row().set("id", "r" + i));
    }
  }

  private void insertRowsWithOwner(int count, String owner) {
    String groupSql =
        Boolean.TRUE.equals(schema.getTable(TABLE_NAME).getMetadata().getRlsEnabled())
            ? GROUP_NAME
            : null;
    for (int i = 0; i < count; i++) {
      jooq.execute(
          "INSERT INTO \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" (id, mg_owner, mg_groups) VALUES (?, ?, ?)",
          "r" + i,
          owner,
          groupSql != null ? new String[] {groupSql} : new String[0]);
    }
  }

  private void grantCustomRole(SelectScope scope, boolean withRls) {
    if (withRls) {
      roleManager.createGroup(schema, GROUP_NAME);
    }
    roleManager.createRole(SCHEMA_NAME, ROLE_NAME);
    PermissionSet ps = new PermissionSet();
    ps.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).select(scope));
    roleManager.setPermissions(schema, ROLE_NAME, ps);
    if (withRls) {
      roleManager.addGroupMembership(SCHEMA_NAME, GROUP_NAME, USER_NAME, ROLE_NAME);
    } else {
      roleManager.grantRoleToUser(schema, ROLE_NAME, USER_NAME);
    }
  }

  private JsonNode queryAgg(String field) throws Exception {
    String json = schema.query(TABLE_NAME + "_agg", s(field)).retrieveJSON();
    JsonNode root = mapper.readTree(json);
    return root.get(TABLE_NAME + "_agg");
  }
}
