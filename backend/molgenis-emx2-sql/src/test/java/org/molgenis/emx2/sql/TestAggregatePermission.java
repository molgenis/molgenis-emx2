package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Privileges.AGGREGATOR;
import static org.molgenis.emx2.SelectColumn.s;
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
import org.molgenis.emx2.datamodels.PetStoreLoader;

public class TestAggregatePermission {
  private static Database db;
  static Schema schema;

  @BeforeAll
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestAggregatePermission.class.getSimpleName());
    new PetStoreLoader().load(schema, true);
    schema.removeMember(ANONYMOUS);
    schema.addMember("AGGREGATE_TEST_USER", AGGREGATOR.toString());
    db.setActiveUser("AGGREGATE_TEST_USER");
  }

  @Test
  public void shouldBeAggregatorRoleOnly() {
    List<String> roles = schema.getInheritedRolesForActiveUser();
    assertEquals(1, roles.size());
    assertEquals(AGGREGATOR.toString(), roles.get(0));
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
  public void testAggregatorCanRetrieveCountsWithMinimum10() {
    assertTrue(schema.query("Pet_agg", s(COUNT_FIELD)).retrieveJSON().contains("10"));
  }

  @Test
  public void testAggregatorPermissionGroupByThresholds() throws JsonProcessingException {
    try {
      AGGREGATE_COUNT_THRESHOLD = 5;
      String json = schema.query("Pet_groupBy", s("count"), s("tags", s("name"))).retrieveJSON();
      Map<String, List<Map<String, Object>>> result = new ObjectMapper().readValue(json, Map.class);
      List<Integer> counts =
          result.get("Pet_groupBy").stream()
              .map(object -> (Integer) object.get(COUNT_FIELD))
              .toList();
      counts.forEach(count -> assertEquals(count, AGGREGATE_COUNT_THRESHOLD));

      json =
          schema
              .query("Pet_groupBy", s(COUNT_FIELD), s(SUM_FIELD, s("weight")), s("tags", s("name")))
              .retrieveJSON();
      assertTrue(json.contains("16.21")); // should be a sum of all 'green'
    } finally {
      AGGREGATE_COUNT_THRESHOLD =
          1; // no other tests affected, but reset just to make sure. Todo: later this becomes a
      // setting.
    }
  }

  @Test
  public void testAggregatorCanGroupByNonOntologyFields() throws JsonProcessingException {
    assertThrows(
        MolgenisException.class,
        () -> schema.query("Pet_groupBy", s("count"), s("category", s("name"))).retrieveJSON());
  }
}
