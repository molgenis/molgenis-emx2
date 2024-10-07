package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.LastUpdate;

class ChangeLogExecutorTest {

  static SqlDatabase sqlDatabase;

  @BeforeEach
  void setUp() {
    sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void getSchemasWithChangeLog() {
    List<String> schemasWithChangeLog =
        ChangeLogExecutor.getSchemasWithChangeLog(sqlDatabase.getJooq());
    assertEquals(
        Arrays.asList(
            new String[] {"testSchemaChangesChangeCount", "testSchemaChanges", "pet store"}),
        schemasWithChangeLog);
  }

  @Test
  void executeLastUpdates() {
    List<LastUpdate> lastUpdates = ChangeLogExecutor.executeLastUpdates(sqlDatabase.getJooq());
    assertEquals(3, lastUpdates.size());
  }
}
