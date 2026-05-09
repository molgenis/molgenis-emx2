package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSchemaTables {

  private static final String SCHEMA_NAME = "TGraphqlSchemaTables";
  private static final String TABLE_ITEM = "Item";
  private static final String TABLE_ITEM_ENABLE = "ItemEnable";
  private static final String TABLE_ITEM_DISABLE_REJECT = "ItemDisableReject";
  private static final String TABLE_ASSET = "Asset";
  private static final String QUERY_TABLES = "{ _schema { tables { name rlsEnabled } } }";

  private static Database database;
  private static Schema schema;
  private static GraphqlExecutor executor;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema
        .getMetadata()
        .create(
            TableMetadata.table(TABLE_ITEM)
                .add(Column.column("name").setType(ColumnType.STRING).setKey(1)));
    schema
        .getMetadata()
        .create(
            TableMetadata.table(TABLE_ITEM_ENABLE)
                .add(Column.column("name").setType(ColumnType.STRING).setKey(1)));
    schema
        .getMetadata()
        .create(
            TableMetadata.table(TABLE_ITEM_DISABLE_REJECT)
                .add(Column.column("name").setType(ColumnType.STRING).setKey(1)));
    executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @AfterAll
  static void tearDown() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void rlsEnabled_readOnFreshTable_isFalse() throws IOException {
    JsonNode tables = queryTablesNode();
    JsonNode itemTable = findTableByName(tables, TABLE_ITEM);
    assertNotNull(itemTable, "Pre-condition: table " + TABLE_ITEM + " must exist");
    assertFalse(
        itemTable.path("rlsEnabled").asBoolean(false), "Fresh table must report rlsEnabled=false");
  }

  @Test
  void rlsEnabled_setTrueViaMutation_persists() throws IOException {
    JsonNode tablesBefore = queryTablesNode();
    JsonNode itemBefore = findTableByName(tablesBefore, TABLE_ITEM_ENABLE);
    assertNotNull(itemBefore, "Pre-condition: table must exist");
    assertFalse(
        itemBefore.path("rlsEnabled").asBoolean(false),
        "Pre-condition: rlsEnabled must be false before mutation");

    execute(
        "mutation { change(tables: [{name: \""
            + TABLE_ITEM_ENABLE
            + "\", rlsEnabled: true}]) { message } }");

    JsonNode tablesAfter = queryTablesNode();
    JsonNode itemAfter = findTableByName(tablesAfter, TABLE_ITEM_ENABLE);
    assertNotNull(itemAfter, "Table must still exist after mutation");
    assertTrue(
        itemAfter.path("rlsEnabled").asBoolean(false), "rlsEnabled must be true after mutation");
  }

  @Test
  void rlsEnabled_disableViaMutation_isRejected() throws IOException {
    execute(
        "mutation { change(tables: [{name: \""
            + TABLE_ITEM_DISABLE_REJECT
            + "\", rlsEnabled: true}]) { message } }");

    JsonNode tablesMid = queryTablesNode();
    JsonNode itemMid = findTableByName(tablesMid, TABLE_ITEM_DISABLE_REJECT);
    assertNotNull(itemMid, "Table must exist after enable mutation");
    assertTrue(
        itemMid.path("rlsEnabled").asBoolean(false),
        "Mid-state: rlsEnabled must be true after enable mutation");

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    "mutation { change(tables: [{name: \""
                        + TABLE_ITEM_DISABLE_REJECT
                        + "\", rlsEnabled: false}]) { message } }"),
            "Disabling RLS via migrate must throw MolgenisException");
    assertTrue(
        ex.getMessage().contains("Cannot disable RLS via schema migration"),
        "Error must mention 'Cannot disable RLS via schema migration'; got: " + ex.getMessage());

    JsonNode tablesAfter = queryTablesNode();
    JsonNode itemAfter = findTableByName(tablesAfter, TABLE_ITEM_DISABLE_REJECT);
    assertNotNull(itemAfter, "Table must still exist after rejected disable");
    assertTrue(
        itemAfter.path("rlsEnabled").asBoolean(false),
        "rlsEnabled must still be true after rejected disable");
  }

  @Test
  void scopeRejectionBubblesThroughGraphql() throws IOException {
    schema
        .getMetadata()
        .create(
            TableMetadata.table(TABLE_ASSET)
                .add(Column.column("code").setType(ColumnType.STRING).setKey(1)));
    executor = new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());

    JsonNode tablesBefore = queryTablesNode();
    JsonNode assetBefore = findTableByName(tablesBefore, TABLE_ASSET);
    assertNotNull(assetBefore, "Pre-condition: table " + TABLE_ASSET + " must exist");
    assertFalse(
        assetBefore.path("rlsEnabled").asBoolean(false),
        "Pre-condition: rlsEnabled must be false before rejection test");

    String rolesBefore =
        convertExecutionResultToJson(
            executor.executeWithoutSession("{ _schema { roles { name } } }"));
    JsonNode rolesBeforeNode = new ObjectMapper().readTree(rolesBefore).at("/data/_schema/roles");
    assertNull(
        findRoleByName(rolesBeforeNode, "rlsRejectRole"),
        "Pre-condition: rlsRejectRole must not exist before rejection test");

    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    "mutation { change(roles: [{name: \"rlsRejectRole\", "
                        + "permissions: [{table: \""
                        + TABLE_ASSET
                        + "\", select: OWN}]}]) { message } }"),
            "Granting OWN scope on non-RLS table must throw MolgenisException");
    assertTrue(
        thrown.getMessage().contains("RLS"), "Error must mention RLS; got: " + thrown.getMessage());

    String rolesAfterJson =
        convertExecutionResultToJson(
            executor.executeWithoutSession("{ _schema { roles { name } } }"));
    JsonNode rolesAfterNode = new ObjectMapper().readTree(rolesAfterJson).at("/data/_schema/roles");
    assertNull(
        findRoleByName(rolesAfterNode, "rlsRejectRole"),
        "rlsRejectRole must not exist after rejected mutation");
  }

  private JsonNode queryTablesNode() throws IOException {
    String json = convertExecutionResultToJson(executor.executeWithoutSession(QUERY_TABLES));
    return new ObjectMapper().readTree(json).at("/data/_schema/tables");
  }

  private void execute(String mutation) throws IOException {
    String json = convertExecutionResultToJson(executor.executeWithoutSession(mutation));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }

  private JsonNode findTableByName(JsonNode tables, String name) {
    if (tables.isArray()) {
      for (JsonNode table : tables) {
        if (name.equals(table.path("name").asText())) {
          return table;
        }
      }
    }
    return null;
  }

  private JsonNode findRoleByName(JsonNode roles, String name) {
    if (roles.isArray()) {
      for (JsonNode role : roles) {
        if (name.equals(role.path("name").asText())) {
          return role;
        }
      }
    }
    return null;
  }
}
