package org.molgenis.emx2.datamodels.beacon;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.ArrayList;
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
  private static final String DEFAULT_SCHEMA_NAME = "BeaconConfigurableTableTestDefault";
  private static final String CUSTOM_TABLE = "Subjects";
  private static final String ENDPOINT = "beacon_" + EntryType.INDIVIDUALS.getName();
  private static final String MARKER_TEMPLATE = "{ \"marker\": \"custom-template\" }";

  /**
   * Reads "subjectCode", a column the packaged individuals template knows nothing about, and emits
   * the standard envelope so it can take part in a cross-schema response.
   */
  private static final String CUSTOM_TEMPLATE =
      """
      {
        "meta": { "beaconId": "custom-beacon" },
        "response": {
          "resultSets": [for (.resultSets) {
            "id": .id,
            "setType": "individuals",
            "resultsCount": .count,
            "results": [for (.results) { "id": .subjectCode }]
          }]
        }
      }
      """;

  private static Database database;
  private static Schema schema;

  @BeforeAll
  void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA_NAME);
    database.dropSchemaIfExists(DEFAULT_SCHEMA_NAME);

    schema = database.createSchema(SCHEMA_NAME);
    schema.create(table(CUSTOM_TABLE, column("id").setPkey(), column("subjectCode")));
    schema
        .getTable(CUSTOM_TABLE)
        .insert(
            row("id", "S1", "subjectCode", "code-1"),
            row("id", "S2", "subjectCode", "code-2"),
            row("id", "S3", "subjectCode", "code-3"));

    // a second schema holding a plain entry type table, so cross-schema queries mix a
    // template-configured schema with one that relies on the packaged template
    Schema defaultSchema = database.createSchema(DEFAULT_SCHEMA_NAME);
    defaultSchema.create(table(EntryType.INDIVIDUALS.getId(), column("id").setPkey()));
    defaultSchema.getTable(EntryType.INDIVIDUALS.getId()).insert(row("id", "D1"), row("id", "D2"));
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
    database.dropSchemaIfExists(DEFAULT_SCHEMA_NAME);
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
  void crossSchemaAppliesEachSchemasOwnJsltTemplate() {
    insertTemplate(
        row(
            "endpoint", ENDPOINT,
            "schema", SCHEMA_NAME,
            "tableName", CUSTOM_TABLE,
            "template", CUSTOM_TEMPLATE));

    JsonNode json = newQuery().query(database);

    // the configured schema is rendered with its own template, so the column that only its
    // template knows about carries a value instead of being dropped as null
    JsonNode configured = resultSetFor(json, SCHEMA_NAME);
    assertNotNull(configured, "cross-schema query should include the configured schema");
    assertEquals(3, configured.get("resultsCount").intValue());
    assertEquals(
        List.of("code-1", "code-2", "code-3"),
        resultIds(configured),
        "results must come from the schema's own template, not the packaged one");

    // a schema without a template is still rendered with the packaged template
    JsonNode packaged = resultSetFor(json, DEFAULT_SCHEMA_NAME);
    assertNotNull(packaged, "cross-schema query should include schemas without a template");
    assertEquals(2, packaged.get("resultsCount").intValue());
    assertEquals(List.of("D1", "D2"), resultIds(packaged));

    // the envelope stays the packaged one: it describes the request, not any one schema's data
    assertEquals("org.molgenis.beaconv2", json.path("meta").path("beaconId").asText());
  }

  @Test
  void crossSchemaRejectsTemplateThatDropsTheResultSets() {
    // a template free to reshape a single-schema response cannot be merged with other schemas
    insertTemplate(
        row(
            "endpoint", ENDPOINT,
            "schema", SCHEMA_NAME,
            "tableName", CUSTOM_TABLE,
            "template", MARKER_TEMPLATE));

    MolgenisException thrown =
        assertThrows(MolgenisException.class, () -> newQuery().query(database));
    assertTrue(
        thrown.getMessage().contains("response.resultSets"),
        "expected the message to name the missing field, got: " + thrown.getMessage());
  }

  @Test
  void templateWithoutTableNameFallsBackToEntryTypeDefault() {
    // a row predating the tableName column: the entry type's own table id must still be used
    insertTemplate(row("endpoint", ENDPOINT, "schema", SCHEMA_NAME, "template", MARKER_TEMPLATE));

    MolgenisException thrown =
        assertThrows(MolgenisException.class, () -> newQuery().query(schema));
    // the message names the table that was resolved, proving the enum default was used
    assertTrue(
        thrown.getMessage().contains(EntryType.INDIVIDUALS.getId()),
        "expected fallback to "
            + EntryType.INDIVIDUALS.getId()
            + " but got: "
            + thrown.getMessage());
  }

  @Test
  void templateForADifferentEndpointIsIgnored() {
    // same schema, but bound to another entry type: must not leak into the individuals query
    insertTemplate(
        row(
            "endpoint", "beacon_" + EntryType.ANALYSES.getName(),
            "schema", SCHEMA_NAME,
            "tableName", CUSTOM_TABLE));

    MolgenisException thrown =
        assertThrows(MolgenisException.class, () -> newQuery().query(schema));
    assertTrue(
        thrown.getMessage().contains(EntryType.INDIVIDUALS.getId()),
        "individuals query must ignore another endpoint's table, got: " + thrown.getMessage());
  }

  @Test
  void savingAnExistingTemplateUpdatesItAndTakesEffectImmediately() {
    saveTemplate(CUSTOM_TABLE, null);
    assertEquals(
        3,
        newQuery()
            .query(schema)
            .get("response")
            .get("resultSets")
            .get(0)
            .get("resultsCount")
            .intValue());

    saveTemplate(CUSTOM_TABLE, MARKER_TEMPLATE);

    assertEquals("custom-template", newQuery().query(schema).path("marker").asText());
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

  private void saveTemplate(String tableName, String template) {
    database.becomeAdmin();
    database
        .getSchema(SYSTEM_SCHEMA)
        .getTable("Templates")
        .save(
            row(
                "endpoint", ENDPOINT,
                "schema", SCHEMA_NAME,
                "tableName", tableName,
                "template", template));
  }

  private QueryEntryType newQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    return new QueryEntryType(new BeaconRequestBody(request));
  }

  /* Top level "id" of each result; not findValuesAsText, which also digs into nested ontology terms. */
  private static List<String> resultIds(JsonNode resultSet) {
    List<String> ids = new ArrayList<>();
    resultSet.get("results").forEach(result -> ids.add(result.path("id").asText()));
    return ids;
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
