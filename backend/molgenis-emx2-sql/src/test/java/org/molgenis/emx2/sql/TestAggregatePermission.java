package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Privileges.AGGREGATOR;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlQuery.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.SQLException;
import java.util.List;
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
  public void testAggregatorCanRetrieveExactCount() {
    String json = schema.query("Pet_agg", s(COUNT_FIELD)).retrieveJSON();
    assertNotNull(json);
    assertFalse(json.contains("\"count\":0"), "AGGREGATE must return exact count, not zero");
  }

  @Test
  public void testAggregatorPermissionGroupBy() throws JsonProcessingException {
    String json = schema.query("Pet_groupBy", s("count"), s("tags", s("name"))).retrieveJSON();
    assertNotNull(json);

    json =
        schema
            .query("Pet_groupBy", s(COUNT_FIELD), s(SUM_FIELD, s("weight")), s("tags", s("name")))
            .retrieveJSON();
    assertTrue(json.contains("16.21"));
  }

  @Test
  public void testAggregatorCanGroupByNonOntologyFields() throws JsonProcessingException {
    assertThrows(
        MolgenisException.class,
        () -> schema.query("Pet_groupBy", s("count"), s("category", s("name"))).retrieveJSON());
  }
}
