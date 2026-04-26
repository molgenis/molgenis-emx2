package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Constants.MOLGENIS_AGGREGATE_COUNT_THRESHOLD;
import static org.molgenis.emx2.Privileges.AGGREGATOR;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlQuery.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public class TestAggregatePermission {
  private static Database db;
  static Schema schema;
  static String schemaName = TestAggregatePermission.class.getSimpleName();

  @BeforeAll
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();

    db.dropSchemaIfExists(schemaName);
    PET_STORE.getImportTask(db, schemaName, "", true).run();
    schema = db.getSchema(schemaName);
    schema.removeMember(ANONYMOUS);
    schema.addMember("AGGREGATE_TEST_USER", AGGREGATOR.toString());
    db.setActiveUser("AGGREGATE_TEST_USER");
  }

  @Test
  public void shouldBeAggregatorRole() {
    List<String> roles = schema.getInheritedRolesForActiveUser();
    assertEquals(3, roles.size());
    assertTrue(roles.contains(AGGREGATOR.toString()));
  }

  @Test
  public void testAggregatorCannotRetrieveRowsUnlessOntology() {
    assertThrows(MolgenisException.class, () -> schema.getTable("Pet").retrieveRows());
    assertDoesNotThrow(() -> schema.getTable("Tag").retrieveRows());
  }

  @Test
  public void testAggregatorCannotRetrieveJson() {
    assertThrows(MolgenisException.class, () -> schema.query("Pet").retrieveJSON());
    assertDoesNotThrow(() -> schema.query("Tag").retrieveJSON());
  }

  @Test
  public void testAggregatorCanRetrieveCountsWithMinimum10() throws JsonProcessingException {
    db.becomeAdmin();
    try {
      db.setSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD, "10");
      db.setActiveUser("AGGREGATE_TEST_USER");
      String json =
          schema.query("Pet_groupBy", s(COUNT_FIELD), s("tags", s("name"))).retrieveJSON();
      Map<String, List<Map<String, Object>>> result = new ObjectMapper().readValue(json, Map.class);
      List<Integer> counts =
          result.get("Pet_groupBy").stream().map(row -> (Integer) row.get(COUNT_FIELD)).toList();
      assertFalse(counts.isEmpty(), "must have at least one group");
      counts.forEach(
          count ->
              assertTrue(count >= 10, "each group count must be >= threshold 10, got: " + count));
    } finally {
      db.becomeAdmin();
      db.removeSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD);
      db.setActiveUser("AGGREGATE_TEST_USER");
    }
  }

  @Test
  public void testAggregatorPermissionGroupByThresholds() throws JsonProcessingException {
    db.becomeAdmin();
    try {
      db.setSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD, "5");
      db.setActiveUser("AGGREGATE_TEST_USER");
      String json = schema.query("Pet_groupBy", s("count"), s("tags", s("name"))).retrieveJSON();
      Map<String, List<Map<String, Object>>> result = new ObjectMapper().readValue(json, Map.class);
      List<Integer> counts =
          result.get("Pet_groupBy").stream()
              .map(object -> (Integer) object.get(COUNT_FIELD))
              .toList();
      counts.forEach(count -> assertTrue(count >= 5, "each group count must be >= threshold 5"));

      json =
          schema
              .query("Pet_groupBy", s(COUNT_FIELD), s(SUM_FIELD, s("weight")), s("tags", s("name")))
              .retrieveJSON();
      // all groups have fewer pets than threshold=5, so all SUMs must be suppressed
      assertTrue(
          json.contains("null"), "SUM must be null for all groups below threshold 5: " + json);
    } finally {
      db.becomeAdmin();
      db.removeSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD);
      db.setActiveUser("AGGREGATE_TEST_USER");
    }
  }

  @Test
  public void threshold_clamps_small_group_count_upward() throws JsonProcessingException {
    // Threshold larger than the total pet count (10) so every group's raw count is below threshold.
    // GREATEST(threshold, COUNT(*)) must return threshold for every group.
    db.becomeAdmin();
    try {
      db.setSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD, "100");
      db.setActiveUser("AGGREGATE_TEST_USER");
      String json =
          schema.query("Pet_groupBy", s(COUNT_FIELD), s("tags", s("name"))).retrieveJSON();
      Map<String, List<Map<String, Object>>> result = new ObjectMapper().readValue(json, Map.class);
      List<Integer> counts =
          result.get("Pet_groupBy").stream()
              .map(object -> (Integer) object.get(COUNT_FIELD))
              .toList();
      assertFalse(counts.isEmpty(), "must have at least one group");
      counts.forEach(
          count -> assertEquals(100, count, "count below threshold must be clamped to 100"));
    } finally {
      db.becomeAdmin();
      db.removeSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD);
      db.setActiveUser("AGGREGATE_TEST_USER");
    }
  }

  @Test
  public void threshold_suppresses_sum_for_groups_below_threshold() throws JsonProcessingException {
    // Threshold larger than total pet count so every group is below threshold.
    // SUM must be suppressed (null) for all groups to prevent re-identification via small groups.
    db.becomeAdmin();
    try {
      db.setSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD, "100");
      db.setActiveUser("AGGREGATE_TEST_USER");
      String json =
          schema
              .query("Pet_groupBy", s(COUNT_FIELD), s(SUM_FIELD, s("weight")), s("tags", s("name")))
              .retrieveJSON();
      assertTrue(
          json.contains("null"),
          "SUM for groups below threshold must be suppressed (null): " + json);
    } finally {
      db.becomeAdmin();
      db.removeSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD);
      db.setActiveUser("AGGREGATE_TEST_USER");
    }
  }

  @Test
  public void threshold_disabled_returns_real_sum() throws JsonProcessingException {
    // When no threshold is set (MIN_VALUE / not configured), SUM must return real values.
    // This verifies B2 fix: threshold removal from DB config fully disables clamping.
    db.becomeAdmin();
    db.removeSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD);
    db.setActiveUser("AGGREGATE_TEST_USER");
    String json =
        schema
            .query("Pet_groupBy", s(COUNT_FIELD), s(SUM_FIELD, s("weight")), s("tags", s("name")))
            .retrieveJSON();
    assertTrue(
        json.contains("16.21"), "SUM must return real value when threshold is disabled: " + json);
  }

  @Test
  public void testAggregatorCanGroupByNonOntologyFields() throws JsonProcessingException {
    assertThrows(
        MolgenisException.class,
        () -> schema.query("Pet_groupBy", s("count"), s("category", s("name"))).retrieveJSON());
  }
}
