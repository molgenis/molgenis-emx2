package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.ENUM;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class SchemaMetadataGraphqlSurfaceTest {

  private static final String SCHEMA_NAME = SchemaMetadataGraphqlSurfaceTest.class.getSimpleName();
  private static GraphqlExecutor graphqlExecutor;
  private static Database database;
  private static Schema schema;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.getSchema(database.createSchema(SCHEMA_NAME).getName());
    graphqlExecutor = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @Test
  void jsonModelInheritIdsHasBothParentsForDiamondChild() throws IOException {
    SchemaMetadata source = new SchemaMetadata();
    source.create(
        table("Root")
            .add(column("id").setType(STRING).setPkey())
            .add(column("rootCol").setType(STRING)));
    source.create(table("ParentB").setInheritNames("Root").add(column("bCol").setType(STRING)));
    source.create(table("ParentC").setInheritNames("Root").add(column("cCol").setType(STRING)));
    source.create(
        table("Child")
            .setInheritNames("ParentB", "ParentC")
            .add(column("childCol").setType(STRING)));

    String json = JsonUtil.schemaToJson(source);
    SchemaMetadata restored = JsonUtil.jsonToSchema(json);

    TableMetadata restoredChild = restored.getTableMetadata("Child");
    assertNotNull(restoredChild, "Table Child must survive JSON round-trip");

    List<String> inheritNames = restoredChild.getInheritNames();
    assertTrue(
        inheritNames.containsAll(List.of("ParentB", "ParentC")),
        "inheritNames must contain both parents after round-trip, got: " + inheritNames);

    org.molgenis.emx2.json.Table jsonTable =
        new org.molgenis.emx2.json.Table(source.getTableMetadata("Child"));
    List<String> inheritIds = jsonTable.getInheritIds();
    assertEquals(2, inheritIds.size(), "inheritIds must have both parents, got: " + inheritIds);
    assertFalse(inheritIds.get(0).isEmpty(), "first inheritId must not be empty");
    assertFalse(inheritIds.get(1).isEmpty(), "second inheritId must not be empty");
    assertNotEquals(
        inheritIds.get(0), inheritIds.get(1), "inheritIds for two distinct parents must differ");
  }

  @Test
  void jsonModelColumnValuesRoundtrip() throws IOException {
    SchemaMetadata source = new SchemaMetadata();
    source.create(
        table("EnumHolder")
            .add(column("id").setType(STRING).setPkey())
            .add(column("status").setType(ENUM).setValues("active", "inactive", "pending")));

    String json = JsonUtil.schemaToJson(source);
    SchemaMetadata restored = JsonUtil.jsonToSchema(json);

    TableMetadata restoredTable = restored.getTableMetadata("EnumHolder");
    assertNotNull(restoredTable, "Table EnumHolder must survive JSON round-trip");

    org.molgenis.emx2.Column restoredCol = restoredTable.getColumn("status");
    assertNotNull(restoredCol, "Column status must survive JSON round-trip");
    assertEquals(
        List.of("active", "inactive", "pending"),
        restoredCol.getValues(),
        "Column values must round-trip through JSON");
  }

  @Test
  void graphqlSurfaceExposesInheritNamesInheritIdsAndColumnValues() throws IOException {
    schema.create(
        table("GqlRoot")
            .add(column("id").setType(STRING).setPkey())
            .add(column("rootCol").setType(STRING)));
    schema.create(
        table("GqlParentB").setInheritNames("GqlRoot").add(column("bCol").setType(STRING)));
    schema.create(
        table("GqlParentC").setInheritNames("GqlRoot").add(column("cCol").setType(STRING)));
    schema.create(
        table("GqlChild")
            .setInheritNames("GqlParentB", "GqlParentC")
            .add(column("childCol").setType(STRING)));

    execute(
        "mutation{change(tables:["
            + "{name:\"GqlEnumHolder\",columns:["
            + "{name:\"id\",columnType:\"STRING\",key:1},"
            + "{name:\"status\",columnType:\"ENUM\",values:[\"active\",\"inactive\",\"pending\"]}"
            + "]}"
            + "]){message}}");

    JsonNode schemaNode =
        execute(
            "{_schema{tables{name,inheritName,inheritId,inheritNames,inheritIds,columns{name,values}}}}");

    JsonNode tables = schemaNode.at("/_schema/tables");

    JsonNode childTable = findTableByName(tables, "GqlChild");
    assertNotNull(childTable, "GqlChild table must appear in _schema");

    String inheritName = childTable.at("/inheritName").asText();
    assertEquals(
        "GqlParentB", inheritName, "inheritName (back-compat scalar) must be primary parent");

    JsonNode inheritNamesNode = childTable.at("/inheritNames");
    assertTrue(inheritNamesNode.isArray(), "inheritNames must be an array");
    assertEquals(2, inheritNamesNode.size(), "inheritNames must list both parents");
    List<String> actualInheritNames =
        List.of(inheritNamesNode.get(0).asText(), inheritNamesNode.get(1).asText());
    assertTrue(
        actualInheritNames.containsAll(List.of("GqlParentB", "GqlParentC")),
        "inheritNames must contain both GqlParentB and GqlParentC, got: " + actualInheritNames);

    JsonNode inheritIdsNode = childTable.at("/inheritIds");
    assertTrue(inheritIdsNode.isArray(), "inheritIds must be an array");
    assertEquals(2, inheritIdsNode.size(), "inheritIds must have both parent identifiers");
    String firstInheritId = inheritIdsNode.get(0).asText();
    String secondInheritId = inheritIdsNode.get(1).asText();
    assertFalse(firstInheritId.isEmpty(), "first inheritId must not be empty");
    assertFalse(secondInheritId.isEmpty(), "second inheritId must not be empty");
    assertNotEquals(firstInheritId, secondInheritId, "two distinct parents must have distinct ids");

    String inheritId = childTable.at("/inheritId").asText();
    assertEquals(
        firstInheritId, inheritId, "inheritId scalar must match first entry in inheritIds");

    JsonNode enumHolder = findTableByName(tables, "GqlEnumHolder");
    assertNotNull(enumHolder, "GqlEnumHolder table must appear in _schema");

    JsonNode statusColumn = findColumnByName(enumHolder.at("/columns"), "status");
    assertNotNull(statusColumn, "status column must appear in _schema");

    JsonNode valuesNode = statusColumn.at("/values");
    assertTrue(valuesNode.isArray(), "column values must be an array");
    assertEquals(3, valuesNode.size(), "column values must have 3 entries");
    List<String> actualValues =
        List.of(valuesNode.get(0).asText(), valuesNode.get(1).asText(), valuesNode.get(2).asText());
    assertEquals(
        List.of("active", "inactive", "pending"), actualValues, "column values must match");
  }

  private JsonNode findTableByName(JsonNode tables, String name) {
    for (JsonNode table : tables) {
      if (name.equals(table.at("/name").asText())) {
        return table;
      }
    }
    return null;
  }

  private JsonNode findColumnByName(JsonNode columns, String name) {
    for (JsonNode column : columns) {
      if (name.equals(column.at("/name").asText())) {
        return column;
      }
    }
    return null;
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphqlExecutor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
