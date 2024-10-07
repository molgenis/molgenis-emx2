package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

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
    assertEquals(List.of(), schemasWithChangeLog);
  }

  @Test
  void executeLastUpdates() {
    List<LastUpdate> lastUpdates = ChangeLogExecutor.executeLastUpdates(sqlDatabase.getJooq());
    assertEquals(0, lastUpdates.size());
  }
}
