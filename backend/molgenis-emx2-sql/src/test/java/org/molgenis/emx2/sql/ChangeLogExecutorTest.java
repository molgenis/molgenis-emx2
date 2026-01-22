package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.IS_CHANGELOG_ENABLED;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.LastUpdate;
import org.molgenis.emx2.MolgenisException;

class ChangeLogExecutorTest {

  static SqlDatabase sqlDatabase;
  private SqlSchema schemaA;

  @BeforeEach
  void setUp() {
    sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    resetTestSchemas();
  }

  private static void resetTestSchemas() {
    sqlDatabase.dropCreateSchema("ChangeLogExecutorTestA");
    sqlDatabase.dropCreateSchema("ChangeLogExecutorTestB");
  }

  private void setupTestData() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(IS_CHANGELOG_ENABLED, "true");
    schemaA = sqlDatabase.getSchema("ChangeLogExecutorTestA");
    schemaA.getMetadata().setSettings(settings);
    sqlDatabase.getSchema("ChangeLogExecutorTestB").getMetadata().setSettings(settings);

    schemaA.create(table("test", column("A").setPkey(), column("B")));
    schemaA.getTable("test").insert(List.of(row("A", "a1", "B", "B")));
  }

  @Test
  void getSchemasWithChangeLog() {
    int nrSchemasWithChangeLog =
        ChangeLogExecutor.getSchemasWithChangeLog(sqlDatabase.getJooq()).size();
    setupTestData();

    List<String> schemasWithChangeLog =
        ChangeLogExecutor.getSchemasWithChangeLog(sqlDatabase.getJooq());
    assertEquals(2, schemasWithChangeLog.size() - nrSchemasWithChangeLog);
    assertTrue(schemasWithChangeLog.contains("ChangeLogExecutorTestA"));
    assertTrue(schemasWithChangeLog.contains("ChangeLogExecutorTestB"));
  }

  @Disabled(
      """
        flaky, sometimes the last changelog entry is from a different schema from a test that is executed prior. It could be
        that the Postgres trigger that is used for generating changelog entries is not fast enough and that the assertion is
        done before the trigger is finished. GetLast might also be too strict, just filter for the expected result.
      """)
  @Test
  void executeLastUpdates() {
    int nrLastUpdates = ChangeLogExecutor.executeLastUpdates(sqlDatabase.getJooq()).size();
    setupTestData();
    List<LastUpdate> lastUpdates = ChangeLogExecutor.executeLastUpdates(sqlDatabase.getJooq());
    assertEquals(1, lastUpdates.size() - nrLastUpdates);

    // Get newest update
    LastUpdate lastUpdate =
        lastUpdates.stream().sorted(Comparator.comparing(LastUpdate::stamp)).toList().getLast();

    assertEquals("ChangeLogExecutorTestA", lastUpdate.schemaName());
  }

  @Test
  void testChangelogLimitCap() {
    setupTestData();
    assertThrows(
        MolgenisException.class,
        () ->
            ChangeLogExecutor.executeGetChanges(
                sqlDatabase.getJooq(), schemaA.getMetadata(), 1001, 0),
        "Requested 1001 changes, but the maximum allowed is "
            + ChangeLogExecutor.CHANGELOG_LIMIT_CAP);
  }
}
