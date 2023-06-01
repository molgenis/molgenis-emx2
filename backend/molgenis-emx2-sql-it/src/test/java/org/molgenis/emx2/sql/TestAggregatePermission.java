package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAggregatePermission {

  private static Database db;
  static Schema schema;
  private static Logger logger =
      LoggerFactory.getLogger(TestCrossSchemaForeignKeysAndInheritance.class);

  @BeforeAll
  public static void setUp() throws SQLException {
    // jdbc:postgresql://mswertz-test-psql1.postgres.database.azure.com:5432/{your_database}?user=molgenis@mswertz-test-psql1&password={your_password}&sslmode=require
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestAggregatePermission.class.getSimpleName());
    new PetStoreLoader().load(schema, true);
  }

  @Test
  public void testAggregatePermission() {
    schema.addMember("aggregatePermissionTester", Privileges.AGGREGATOR.toString());
    db.setActiveUser("aggregatePermissionTester");
    schema = db.getSchema(TestAggregatePermission.class.getSimpleName());

    // should not be able to query rows
    try {
      schema.getTable("Pet").retrieveRows();
      fail("As aggregator should not be able to query tables");
    } catch (Exception e) {
      logger.info("Errored correctly: " + e.getMessage());
    }
  }
}
