package org.molgenis.emx2.datamodels.beacon;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
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
  private static final String ENDPOINT = "beacon_" + EntryType.INDIVIDUALS.getName();
  private static final String MARKER_TEMPLATE = "{ \"marker\": \"custom-template\" }";

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

  @AfterEach
  void removeTemplateRows() {
    database.becomeAdmin();
    Table templates = database.getSchema(SYSTEM_SCHEMA).getTable("Templates");
    List<Row> rows =
        templates.retrieveRows().stream()
            .filter(r -> SCHEMA_NAME.equals(r.getString("schema")))
            .toList();
    if (!rows.isEmpty()) {
      templates.delete(rows);
    }
  }

  @AfterAll
  void cleanup() {
    database.becomeAdmin();
    // dropping the schema cascades through the foreign key and removes any Templates rows
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void queriesConfiguredTableInsteadOfEnumDefault() {
    insertTemplate(row("endpoint", ENDPOINT, "schema", SCHEMA_NAME, "tableName", CUSTOM_TABLE));

    JsonNode json = newQuery().query(schema);
    JsonNode resultSet = json.get("response").get("resultSets").get(0);
    // proves the query targeted the configured "Subjects" table (3 rows) and not the
    // non-existent "Individuals" table that the enum would otherwise resolve to
    assertEquals(3, resultSet.get("resultsCount").intValue());
    assertEquals(3, resultSet.get("results").size());
  }

  @Test
  void appliesConfiguredJsltTemplate_singleSchema() {
    insertTemplate(
        row(
            "endpoint", ENDPOINT,
            "schema", SCHEMA_NAME,
            "tableName", CUSTOM_TABLE,
            "template", MARKER_TEMPLATE));

    JsonNode json = newQuery().query(schema);
    assertEquals("custom-template", json.path("marker").asText());
    assertFalse(json.has("response"));
  }

  @Test
  void crossSchemaHonorsConfiguredTableName() {
    insertTemplate(row("endpoint", ENDPOINT, "schema", SCHEMA_NAME, "tableName", CUSTOM_TABLE));

    JsonNode json = newQuery().query(database);
    JsonNode resultSet = resultSetFor(json, SCHEMA_NAME);
    assertNotNull(resultSet, "cross-schema query should include the configured schema");
    assertEquals(3, resultSet.get("resultsCount").intValue());
  }

  @Test
  void crossSchemaIgnoresConfiguredJsltTemplate() {
    insertTemplate(
        row(
            "endpoint", ENDPOINT,
            "schema", SCHEMA_NAME,
            "tableName", CUSTOM_TABLE,
            "template", MARKER_TEMPLATE));

    JsonNode json = newQuery().query(database);
    assertFalse(json.has("marker"));
    assertTrue(json.has("response"));
  }

  @Test
  void missingTableThrowsForSingleSchemaButIsSkippedCrossSchema() {
    assertThrows(MolgenisException.class, () -> newQuery().query(schema));

    JsonNode json = assertDoesNotThrow(() -> newQuery().query(database));
    assertNull(resultSetFor(json, SCHEMA_NAME));
  }

  private void insertTemplate(Row row) {
    database.becomeAdmin();
    database.getSchema(SYSTEM_SCHEMA).getTable("Templates").insert(row);
  }

  private QueryEntryType newQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    return new QueryEntryType(new BeaconRequestBody(request));
  }

  private static JsonNode resultSetFor(JsonNode response, String schemaId) {
    for (JsonNode resultSet : response.get("response").get("resultSets")) {
      if (schemaId.equals(resultSet.path("id").asText())) {
        return resultSet;
      }
    }
    return null;
  }
}
