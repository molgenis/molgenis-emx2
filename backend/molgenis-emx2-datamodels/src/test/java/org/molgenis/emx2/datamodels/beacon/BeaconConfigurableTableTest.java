package org.molgenis.emx2.datamodels.beacon;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * Verifies that the table a Beacon entry type queries can be configured through the _SYSTEM_
 * .Templates table (via the tableName reference to MOLGENIS.table_metadata) instead of the table
 * name hardcoded in {@link EntryType}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeaconConfigurableTableTest {

  private static final String SCHEMA_NAME = "BeaconConfigurableTableTest";
  private static final String CUSTOM_TABLE = "Subjects";
  private static Database database;
  private static Schema schema;

  @BeforeAll
  void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema.create(table(CUSTOM_TABLE, column("id").setPkey()));
    schema.getTable(CUSTOM_TABLE).insert(row("id", "S1"), row("id", "S2"), row("id", "S3"));
  }

  @AfterAll
  void cleanup() {
    database.becomeAdmin();
    // dropping the schema cascades through the foreign key and removes the Templates row
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void queriesConfiguredTableInsteadOfEnumDefault() {
    database.becomeAdmin();
    Schema systemSchema = database.getSchema(SYSTEM_SCHEMA);
    systemSchema
        .getTable("Templates")
        .insert(
            row(
                "endpoint", "beacon_" + EntryType.INDIVIDUALS.getName(),
                "schema", SCHEMA_NAME,
                "tableName", CUSTOM_TABLE));

    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    QueryEntryType queryEntryType = new QueryEntryType(new BeaconRequestBody(request));
    JsonNode json = queryEntryType.query(database.getSchema(SCHEMA_NAME));

    JsonNode resultSet = json.get("response").get("resultSets").get(0);
    // proves the query targeted the configured "Subjects" table (3 rows) and not the
    // non-existent "Individuals" table that the enum would otherwise resolve to
    assertEquals(3, resultSet.get("resultsCount").intValue());
    assertEquals(3, resultSet.get("results").size());
  }
}
