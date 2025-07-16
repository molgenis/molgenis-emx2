package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.IS_CHANGELOG_ENABLED;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.LastUpdate;

class ChangeLogExecutorTest {

  static SqlDatabase sqlDatabase;

  @BeforeEach
  void setUp() {
    sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

    sqlDatabase.dropCreateSchema("ChangeLogExecutorTestA");
    sqlDatabase.dropCreateSchema("ChangeLogExecutorTestB");

    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(IS_CHANGELOG_ENABLED, "true");
    SqlSchema schemaA = sqlDatabase.getSchema("ChangeLogExecutorTestA");
    schemaA.getMetadata().setSettings(settings);
    sqlDatabase.getSchema("ChangeLogExecutorTestB").getMetadata().setSettings(settings);

    sqlDatabase.clearCache();
    schemaA = sqlDatabase.getSchema("ChangeLogExecutorTestA");
    schemaA.create(table("test", column("A").setPkey(), column("B")));
    schemaA.getTable("test").insert(List.of(row("A", "a1", "B", "B")));
  }

  @Test
  void getSchemasWithChangeLog() {
    List<String> schemasWithChangeLog =
        ChangeLogExecutor.getSchemasWithChangeLog(sqlDatabase.getJooq());
    assertTrue(schemasWithChangeLog.contains("ChangeLogExecutorTestA"));
    assertTrue(schemasWithChangeLog.contains("ChangeLogExecutorTestB"));
  }

  @Test
  void executeLastUpdates() {
    List<LastUpdate> lastUpdates = ChangeLogExecutor.executeLastUpdates(sqlDatabase.getJooq());
    assertEquals(1, lastUpdates.size());
    LastUpdate lastUpdate = lastUpdates.get(0);
    assertEquals("ChangeLogExecutorTestA", lastUpdate.schemaName());
  }
}
