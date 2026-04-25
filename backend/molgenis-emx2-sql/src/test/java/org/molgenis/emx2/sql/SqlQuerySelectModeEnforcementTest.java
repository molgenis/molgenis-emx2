package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;

/**
 * Story 3.7 — enforce SelectScope mode at query time (RED → GREEN).
 *
 * <p>Capability matrix (from spec Phase 3):
 *
 * <pre>
 * Mode      | rows | count      | sum/avg | min/max | groupBy | exists
 * ----------|------|------------|---------|---------|---------|-------
 * ALL       |  ✓   | ✓ exact    |   ✓     |   ✓     |   ✓     |   ✓
 * AGGREGATE |  ✗   | ✓ exact    |   ✓     |   ✓     |   ✓     |   ✓
 * COUNT     |  ✗   | ✓ exact    |   ✗     |   ✗     |   ✗     |   ✓
 * EXISTS    |  ✗   |     ✗      |   ✗     |   ✗     |   ✗     |   ✓
 * RANGE     |  ✗   | ✓ truncated|   ✗     |   ✓     |   ✗     |   ✓
 * </pre>
 */
class SqlQuerySelectModeEnforcementTest {

  private static final String SCHEMA_NAME = "SelectModeEnforceTest";
  private static final String DATA_TABLE = "Items";

  private static final String USER_EXISTS = "sme_user_exists";
  private static final String USER_COUNT = "sme_user_count";
  private static final String USER_AGGREGATE = "sme_user_aggregate";
  private static final String USER_RANGE = "sme_user_range";
  private static final String USER_ALL = "sme_user_all";

  private static final String ROLE_EXISTS = "sme role exists";
  private static final String ROLE_COUNT = "sme role count";
  private static final String ROLE_AGGREGATE = "sme role aggregate";
  private static final String ROLE_RANGE = "sme role range";
  private static final String ROLE_ALL = "sme role all";

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private Schema schema;

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

    for (String user : List.of(USER_EXISTS, USER_COUNT, USER_AGGREGATE, USER_RANGE, USER_ALL)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    grantRole(ROLE_EXISTS, USER_EXISTS, SelectScope.EXISTS);
    grantRole(ROLE_COUNT, USER_COUNT, SelectScope.COUNT);
    grantRole(ROLE_AGGREGATE, USER_AGGREGATE, SelectScope.AGGREGATE);
    grantRole(ROLE_RANGE, USER_RANGE, SelectScope.RANGE);
    grantRole(ROLE_ALL, USER_ALL, SelectScope.ALL);
  }

  private void grantRole(String roleName, String user, SelectScope selectMode) {
    schema.createRole(roleName);
    schema.grant(
        roleName,
        new TablePermission(
            null,
            DATA_TABLE,
            TablePermission.singletonSelect(selectMode),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(user, roleName);
  }

  @AfterEach
  void tearDown() {
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  // --- EXISTS mode ---

  @Test
  void exists_allowsExistsCheck() {
    database.setActiveUser(USER_EXISTS);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.EXISTS_FIELD)).retrieveJSON();
    assertNotNull(json, "exists check must return a result");
    assertTrue(json.contains("true"), "5 rows exist — exists must be true");
  }

  @Test
  void exists_blocksRowFetch() {
    database.setActiveUser(USER_EXISTS);
    assertThrows(
        MolgenisException.class,
        () -> schema.getTable(DATA_TABLE).retrieveRows(),
        "EXISTS mode must block row fetch");
  }

  @Test
  void exists_blocksCount() {
    database.setActiveUser(USER_EXISTS);
    assertThrows(
        MolgenisException.class,
        () -> schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON(),
        "EXISTS mode must block count");
  }

  @Test
  void exists_blocksMinMax() {
    database.setActiveUser(USER_EXISTS);
    assertThrows(
        MolgenisException.class,
        () -> schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON(),
        "EXISTS mode must block min");
  }

  // --- COUNT mode ---

  @Test
  void count_allowsCount() {
    database.setActiveUser(USER_COUNT);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON();
    assertNotNull(json);
    assertTrue(json.contains("5"), "COUNT mode must return exact count of 5");
  }

  @Test
  void count_allowsExistsCheck() {
    database.setActiveUser(USER_COUNT);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.EXISTS_FIELD)).retrieveJSON();
    assertNotNull(json);
    assertTrue(json.contains("true"));
  }

  @Test
  void count_blocksRowFetch() {
    database.setActiveUser(USER_COUNT);
    assertThrows(
        MolgenisException.class,
        () -> schema.getTable(DATA_TABLE).retrieveRows(),
        "COUNT mode must block row fetch");
  }

  @Test
  void count_blocksMinMax() {
    database.setActiveUser(USER_COUNT);
    assertThrows(
        MolgenisException.class,
        () -> schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON(),
        "COUNT mode must block min/max");
  }

  @Test
  void count_blocksAvg() {
    database.setActiveUser(USER_COUNT);
    assertThrows(
        MolgenisException.class,
        () -> schema.query(DATA_TABLE + "_agg", s(SqlQuery.AVG_FIELD, s("weight"))).retrieveJSON(),
        "COUNT mode must block avg");
  }

  @Test
  void count_blocksGroupBy() {
    database.setActiveUser(USER_COUNT);
    assertThrows(
        MolgenisException.class,
        () ->
            schema
                .query(DATA_TABLE + "_groupBy", s(SqlQuery.COUNT_FIELD), s("name"))
                .retrieveJSON(),
        "COUNT mode must block groupBy");
  }

  // --- AGGREGATE mode ---

  @Test
  void aggregate_blocksRowFetch() {
    database.setActiveUser(USER_AGGREGATE);
    assertThrows(
        MolgenisException.class,
        () -> schema.getTable(DATA_TABLE).retrieveRows(),
        "AGGREGATE mode must block row fetch");
  }

  @Test
  void aggregate_allowsExactCount() {
    database.setActiveUser(USER_AGGREGATE);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON();
    assertNotNull(json);
    assertTrue(json.contains("5"), "AGGREGATE count must be exact (5 rows)");
  }

  @Test
  void aggregate_allowsMinMax() {
    database.setActiveUser(USER_AGGREGATE);
    String json =
        schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON();
    assertNotNull(json, "AGGREGATE mode must allow min");
    assertTrue(json.contains("1.5"), "min weight of 5 items is 1.5");
  }

  @Test
  void aggregate_allowsAvg() {
    database.setActiveUser(USER_AGGREGATE);
    String json =
        schema.query(DATA_TABLE + "_agg", s(SqlQuery.AVG_FIELD, s("weight"))).retrieveJSON();
    assertNotNull(json, "AGGREGATE mode must allow avg");
  }

  @Test
  void aggregate_allowsGroupBy() {
    database.setActiveUser(USER_AGGREGATE);
    String json =
        schema.query(DATA_TABLE + "_groupBy", s(SqlQuery.COUNT_FIELD), s("name")).retrieveJSON();
    assertNotNull(json, "AGGREGATE mode must allow groupBy");
  }

  // --- RANGE mode ---

  @Test
  void range_allowsMinMax() {
    database.setActiveUser(USER_RANGE);
    String json =
        schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON();
    assertNotNull(json, "RANGE mode must allow min");
    assertTrue(json.contains("1.5"), "min weight is 1.5");
  }

  @Test
  void range_allowsExistsCheck() {
    database.setActiveUser(USER_RANGE);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.EXISTS_FIELD)).retrieveJSON();
    assertNotNull(json);
    assertTrue(json.contains("true"));
  }

  @Test
  void range_blocksRowFetch() {
    database.setActiveUser(USER_RANGE);
    assertThrows(
        MolgenisException.class,
        () -> schema.getTable(DATA_TABLE).retrieveRows(),
        "RANGE mode must block row fetch");
  }

  @Test
  void range_blocksAvg() {
    database.setActiveUser(USER_RANGE);
    assertThrows(
        MolgenisException.class,
        () -> schema.query(DATA_TABLE + "_agg", s(SqlQuery.AVG_FIELD, s("weight"))).retrieveJSON(),
        "RANGE mode must block avg");
  }

  @Test
  void range_allowsTruncatedCount() {
    database.setActiveUser(USER_RANGE);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON();
    assertNotNull(json);
    assertTrue(json.contains("0"), "RANGE count of 5 rows truncates to 0 (< 10 threshold)");
  }

  @Test
  void range_blocksGroupBy() {
    database.setActiveUser(USER_RANGE);
    assertThrows(
        MolgenisException.class,
        () ->
            schema
                .query(DATA_TABLE + "_groupBy", s(SqlQuery.COUNT_FIELD), s("name"))
                .retrieveJSON(),
        "RANGE mode must block groupBy");
  }

  // --- ALL mode (regression guard) ---

  @Test
  void all_rowFetchUnrestricted() {
    database.setActiveUser(USER_ALL);
    List<Row> rows = schema.getTable(DATA_TABLE).retrieveRows();
    assertEquals(5, rows.size(), "ALL mode must allow row fetch of all 5 rows");
  }

  @Test
  void all_countUnrestricted() {
    database.setActiveUser(USER_ALL);
    String json = schema.query(DATA_TABLE + "_agg", s(SqlQuery.COUNT_FIELD)).retrieveJSON();
    assertTrue(json.contains("5"), "ALL mode must return exact count of 5");
  }

  @Test
  void all_minMaxUnrestricted() {
    database.setActiveUser(USER_ALL);
    String json =
        schema.query(DATA_TABLE + "_agg", s(SqlQuery.MIN_FIELD, s("weight"))).retrieveJSON();
    assertTrue(json.contains("1.5"), "ALL mode must allow min");
  }
}
