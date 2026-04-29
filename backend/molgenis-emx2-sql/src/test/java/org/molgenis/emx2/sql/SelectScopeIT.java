package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.molgenis.emx2.*;
import org.molgenis.emx2.TablePermission.SelectScope;

/**
 * Story 3.10 — full matrix integration test via SqlRoleManager.setPermissions (global roles).
 *
 * <p>Complements SqlQuerySelectModeEnforcementTest (schema-scoped roles) by wiring permissions
 * through the global SqlRoleManager path end-to-end.
 *
 * <p>Capability matrix:
 *
 * <pre>
 * Mode      | rows | count        | sum/avg  | min/max  | groupBy  | exists
 * ----------|------|--------------|----------|----------|----------|-------
 * ALL       |  ✓   | ✓ exact      |    ✓     |    ✓     |    ✓     |   ✓
 * AGGREGATE |  ✗   | ✓ exact      |    ✓     |    ✓     |    ✓     |   ✓
 * COUNT     |  ✗   | ✓ exact      |    ✗     |    ✗     |    ✗     |   ✓
 * EXISTS    |  ✗   |    ✗         |    ✗     |    ✗     |    ✗     |   ✓
 * RANGE     |  ✗   | ✓ truncated  |    ✗     |    ✓     |    ✗     |   ✓
 * </pre>
 */
class SelectScopeIT {

  private static final String SCHEMA_NAME = "SelectScopeITSchema";
  private static final String DATA_TABLE = "Items";

  private static final String ROLE_ALL = "ssit role all";
  private static final String ROLE_AGGREGATE = "ssit role aggregate";
  private static final String ROLE_COUNT = "ssit role count";
  private static final String ROLE_EXISTS = "ssit role exists";
  private static final String ROLE_RANGE = "ssit role range";

  private static final String USER_ALL = "ssit_user_all";
  private static final String USER_AGGREGATE = "ssit_user_aggregate";
  private static final String USER_COUNT = "ssit_user_count";
  private static final String USER_EXISTS = "ssit_user_exists";
  private static final String USER_RANGE = "ssit_user_range";

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private static final SqlRoleManager roleManager = new SqlRoleManager(database);

  private Schema schema;

  enum Mode {
    ALL(ROLE_ALL, USER_ALL, SelectScope.ALL),
    AGGREGATE(ROLE_AGGREGATE, USER_AGGREGATE, SelectScope.AGGREGATE),
    COUNT(ROLE_COUNT, USER_COUNT, SelectScope.COUNT),
    EXISTS(ROLE_EXISTS, USER_EXISTS, SelectScope.EXISTS),
    RANGE(ROLE_RANGE, USER_RANGE, SelectScope.RANGE);

    final String role;
    final String user;
    final SelectScope select;

    Mode(String role, String user, SelectScope select) {
      this.role = role;
      this.user = user;
      this.select = select;
    }
  }

  @BeforeEach
  void setUp() {
    database.becomeAdmin();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table(DATA_TABLE)
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("weight").setType(ColumnType.DECIMAL)));

    for (int i = 1; i <= 5; i++) {
      schema
          .getTable(DATA_TABLE)
          .insert(
              new Row()
                  .setString("id", "r" + i)
                  .setString("name", "item" + i)
                  .setDecimal("weight", i * 1.5));
    }

    for (Mode mode : Mode.values()) {
      if (!database.hasUser(mode.user)) {
        database.addUser(mode.user);
      }
      if (!roleManager.listRoles().stream().anyMatch(r -> r.name().equals(mode.role))) {
        roleManager.createRole(mode.role, null);
      }
      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(SCHEMA_NAME, DATA_TABLE)
              .select(TablePermission.singletonSelect(mode.select)));
      roleManager.setPermissions(mode.role, ps);
      roleManager.grantRoleToUser(mode.role, mode.user);
    }
  }

  @AfterEach
  void tearDown() {
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
    for (Mode mode : Mode.values()) {
      try {
        roleManager.revokeRoleFromUser(mode.role, mode.user);
      } catch (Exception ignored) {
        // user may not be member
      }
      PermissionSet empty = new PermissionSet();
      try {
        roleManager.setPermissions(mode.role, empty);
      } catch (Exception ignored) {
        // ignore if role already gone
      }
      try {
        roleManager.deleteRole(mode.role);
      } catch (Exception ignored) {
        // ignore if already deleted
      }
    }
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void rowFetch_allowedOnlyForAllMode(Mode mode) {
    database.setActiveUser(mode.user);
    boolean rowsAllowed = mode == Mode.ALL;
    if (rowsAllowed) {
      List<Row> rows = schema.getTable(DATA_TABLE).retrieveRows();
      assertEquals(5, rows.size());
    } else {
      assertThrows(
          MolgenisException.class,
          () -> schema.getTable(DATA_TABLE).retrieveRows(),
          mode + " must block row fetch");
    }
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void existsCheck_allowedForAllModes(Mode mode) {
    database.setActiveUser(mode.user);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.EXISTS_FIELD)).retrieveJSON();
    assertNotNull(json, mode + " must allow exists check");
    assertTrue(json.contains("true"), mode + ": 5 rows exist so exists must be true");
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void count_allowedForAllCountAggregateAndRange(Mode mode) throws Exception {
    database.setActiveUser(mode.user);
    boolean countAllowed =
        mode == Mode.ALL || mode == Mode.COUNT || mode == Mode.AGGREGATE || mode == Mode.RANGE;
    if (countAllowed) {
      String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON();
      JsonNode root = new ObjectMapper().readTree(json);
      int count = root.path(DATA_TABLE + "_agg").path(SqlQuery.COUNT_FIELD).asInt(-1);
      if (mode == Mode.RANGE) {
        assertEquals(10, count, mode + ": RANGE count of 5 rows rounds up to 10 (CEIL(5/10)*10)");
      } else {
        assertEquals(5, count, mode + ": exact count of 5 rows");
      }
    } else {
      assertThrows(
          MolgenisException.class,
          () -> schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON(),
          mode + " must block count");
    }
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void minMax_allowedForAllAggregateAndRangeOnly(Mode mode) {
    database.setActiveUser(mode.user);
    boolean minMaxAllowed = mode == Mode.ALL || mode == Mode.AGGREGATE || mode == Mode.RANGE;
    if (minMaxAllowed) {
      String json =
          schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON();
      assertNotNull(json, mode + " must allow min/max");
      assertTrue(json.contains("1.5"), mode + ": min weight of 5 items is 1.5");
    } else {
      assertThrows(
          MolgenisException.class,
          () ->
              schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON(),
          mode + " must block min/max");
    }
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void avgSum_allowedForAllAndAggregateOnly(Mode mode) {
    database.setActiveUser(mode.user);
    boolean avgAllowed = mode == Mode.ALL || mode == Mode.AGGREGATE;
    if (avgAllowed) {
      String json =
          schema.query(DATA_TABLE + "_agg", s(SqlQuery.AVG_FIELD, s("weight"))).retrieveJSON();
      assertNotNull(json, mode + " must allow avg");
    } else {
      assertThrows(
          MolgenisException.class,
          () ->
              schema.query(DATA_TABLE + "_agg", s(SqlQuery.AVG_FIELD, s("weight"))).retrieveJSON(),
          mode + " must block avg");
    }
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void groupBy_allowedForAllAndAggregateOnly(Mode mode) {
    database.setActiveUser(mode.user);
    boolean groupByAllowed = mode == Mode.ALL || mode == Mode.AGGREGATE;
    if (groupByAllowed) {
      String json =
          schema.query(DATA_TABLE + "_groupBy", s(SqlQuery.COUNT_FIELD), s("name")).retrieveJSON();
      assertNotNull(json, mode + " must allow groupBy");
    } else {
      assertThrows(
          MolgenisException.class,
          () ->
              schema
                  .query(DATA_TABLE + "_groupBy", s(SqlQuery.COUNT_FIELD), s("name"))
                  .retrieveJSON(),
          mode + " must block groupBy");
    }
  }
}
