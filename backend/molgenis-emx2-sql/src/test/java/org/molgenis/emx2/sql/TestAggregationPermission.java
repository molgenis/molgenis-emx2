package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlQuery.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

class TestAggregationPermission {

  private static final String SCHEMA_NAME = "APTest";
  private static final String TABLE_NAME = "Obs";
  private static final String NUMERIC_COLUMN = "val";
  private static final String CUSTOM_ROLE = "researcher";
  private static final String TEST_USER = "APTestUser";

  private static final SqlDatabase db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private static final SqlRoleManager roleManager = new SqlRoleManager(db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME, column("id").setPkey(), column(NUMERIC_COLUMN).setType(INT)));
    schema.getTable(TABLE_NAME).insert(new Row().set("id", "r1").set(NUMERIC_COLUMN, 10));
    schema.getTable(TABLE_NAME).insert(new Row().set("id", "r2").set(NUMERIC_COLUMN, 20));
    if (!db.hasUser(TEST_USER)) db.addUser(TEST_USER);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private static Stream<Arguments> minMaxAllowed() {
    return Stream.of(
        Arguments.of(SelectScope.ALL),
        Arguments.of(SelectScope.OWN),
        Arguments.of(SelectScope.GROUP),
        Arguments.of(SelectScope.AGGREGATE),
        Arguments.of(SelectScope.RANGE));
  }

  private static Stream<Arguments> minMaxDenied() {
    return Stream.of(
        Arguments.of(SelectScope.COUNT),
        Arguments.of(SelectScope.EXISTS),
        Arguments.of(SelectScope.NONE));
  }

  private static Stream<Arguments> avgSumAllowed() {
    return Stream.of(
        Arguments.of(SelectScope.ALL),
        Arguments.of(SelectScope.OWN),
        Arguments.of(SelectScope.GROUP),
        Arguments.of(SelectScope.AGGREGATE));
  }

  private static Stream<Arguments> avgSumDenied() {
    return Stream.of(
        Arguments.of(SelectScope.COUNT),
        Arguments.of(SelectScope.RANGE),
        Arguments.of(SelectScope.EXISTS),
        Arguments.of(SelectScope.NONE));
  }

  private static Stream<Arguments> groupByAllowed() {
    return Stream.of(
        Arguments.of(SelectScope.ALL),
        Arguments.of(SelectScope.OWN),
        Arguments.of(SelectScope.GROUP),
        Arguments.of(SelectScope.AGGREGATE));
  }

  private static Stream<Arguments> groupByDenied() {
    return Stream.of(
        Arguments.of(SelectScope.COUNT),
        Arguments.of(SelectScope.RANGE),
        Arguments.of(SelectScope.EXISTS),
        Arguments.of(SelectScope.NONE));
  }

  @ParameterizedTest
  @MethodSource("minMaxAllowed")
  void minField_allowed(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryMin());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("minMaxDenied")
  void minField_denied(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, () -> queryMin());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("minMaxAllowed")
  void maxField_allowed(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryMax());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("minMaxDenied")
  void maxField_denied(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, () -> queryMax());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("avgSumAllowed")
  void avgField_allowed(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryAvg());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("avgSumDenied")
  void avgField_denied(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, () -> queryAvg());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("avgSumAllowed")
  void sumField_allowed(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> querySum());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("avgSumDenied")
  void sumField_denied(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, () -> querySum());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("groupByAllowed")
  void groupBy_allowed(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryGroupBy());
    } finally {
      db.becomeAdmin();
    }
  }

  @ParameterizedTest
  @MethodSource("groupByDenied")
  void groupBy_denied(SelectScope scope) {
    grantCustomRole(scope);
    db.setActiveUser(TEST_USER);
    try {
      assertThrows(MolgenisException.class, () -> queryGroupBy());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleViewer_minField_allowed() {
    schema.addMember(TEST_USER, "Viewer");
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryMin());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleViewer_maxField_allowed() {
    schema.addMember(TEST_USER, "Viewer");
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryMax());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleViewer_avgField_allowed() {
    schema.addMember(TEST_USER, "Viewer");
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryAvg());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleViewer_sumField_allowed() {
    schema.addMember(TEST_USER, "Viewer");
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> querySum());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void systemRoleViewer_groupBy_allowed() {
    schema.addMember(TEST_USER, "Viewer");
    db.setActiveUser(TEST_USER);
    try {
      assertDoesNotThrow(() -> queryGroupBy());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void adminNoCustomRole_minField_allowed() {
    db.becomeAdmin();
    assertDoesNotThrow(() -> queryMin());
  }

  @Test
  void adminNoCustomRole_maxField_allowed() {
    db.becomeAdmin();
    assertDoesNotThrow(() -> queryMax());
  }

  @Test
  void adminNoCustomRole_avgField_allowed() {
    db.becomeAdmin();
    assertDoesNotThrow(() -> queryAvg());
  }

  @Test
  void adminNoCustomRole_sumField_allowed() {
    db.becomeAdmin();
    assertDoesNotThrow(() -> querySum());
  }

  @Test
  void adminNoCustomRole_groupBy_allowed() {
    db.becomeAdmin();
    assertDoesNotThrow(() -> queryGroupBy());
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
    if (scope == SelectScope.OWN || scope == SelectScope.GROUP) {
      ((SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata()).setRlsEnabled(true);
    }
    roleManager.createRole(schema, CUSTOM_ROLE, "test role");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new PermissionSet.TablePermissions().setSelect(scope));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);
  }

  private String queryMin() {
    return schema.query(TABLE_NAME + "_agg", s(MIN_FIELD, s(NUMERIC_COLUMN))).retrieveJSON();
  }

  private String queryMax() {
    return schema.query(TABLE_NAME + "_agg", s(MAX_FIELD, s(NUMERIC_COLUMN))).retrieveJSON();
  }

  private String queryAvg() {
    return schema.query(TABLE_NAME + "_agg", s(AVG_FIELD, s(NUMERIC_COLUMN))).retrieveJSON();
  }

  private String querySum() {
    return schema.query(TABLE_NAME + "_agg", s(SUM_FIELD, s(NUMERIC_COLUMN))).retrieveJSON();
  }

  private String queryGroupBy() {
    return schema
        .query(TABLE_NAME + "_groupBy", s(COUNT_FIELD), s(SUM_FIELD, s(NUMERIC_COLUMN)))
        .retrieveJSON();
  }
}
