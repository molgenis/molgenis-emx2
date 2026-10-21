package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.ENUM;
import static org.molgenis.emx2.ColumnType.ENUM_ARRAY;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlEnumColumns {

  private static final String SCHEMA_NAME = TestGraphqlEnumColumns.class.getSimpleName();
  private static final String COLUMNS_QUERY =
      "{_schema{tables{name,columns{name,columnType,values}}}}";
  private static final String SAMPLE_IDS_QUERY = "{Sample{id}}";

  private static Database database;
  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Sample")
            .add(column("id").setPkey())
            .add(column("answer").setType(ENUM).setAllowedValues("yes", "no", "unknown"))
            .add(column("colors").setType(ENUM_ARRAY).setAllowedValues("red", "green", "blue")));
    schema
        .getTable("Sample")
        .insert(
            row("id", "s1", "answer", "yes", "colors", "red,blue"),
            row("id", "s2", "answer", "no", "colors", "green"));
    graphql = new GraphqlExecutor(schema);
  }

  @Test
  void schemaChangeStoresValuesInOrderAndSchemaQueryReturnsThem() throws IOException {
    assertFalse(execute(COLUMNS_QUERY).toString().contains("\"name\":\"Created\""));

    execute(
        """
        mutation{change(tables:[{name:"Created",columns:[
          {name:"id",key:1},
          {name:"level",columnType:"ENUM",values:["low","medium","high"]},
          {name:"flags",columnType:"ENUM_ARRAY",values:["z","a","m"]}
        ]}]){message}}""");

    database.clearCache();
    graphql = new GraphqlExecutor(database.getSchema(SCHEMA_NAME));
    String columns = execute(COLUMNS_QUERY).toString();
    assertTrue(
        columns.contains(
            "\"name\":\"level\",\"columnType\":\"ENUM\",\"values\":[\"low\",\"medium\",\"high\"]"),
        columns);
    assertTrue(
        columns.contains(
            "\"name\":\"flags\",\"columnType\":\"ENUM_ARRAY\",\"values\":[\"z\",\"a\",\"m\"]"),
        columns);
  }

  @Test
  void schemaChangeWithEnumWithoutValuesFailsAndCreatesNothing() throws IOException {
    assertFalse(execute(COLUMNS_QUERY).toString().contains("\"name\":\"NoValues\""));

    assertThrows(
        MolgenisException.class,
        () ->
            execute(
                """
                mutation{change(tables:[{name:"NoValues",columns:[
                  {name:"id",key:1},{name:"level",columnType:"ENUM"}
                ]}]){message}}"""));

    assertFalse(execute(COLUMNS_QUERY).toString().contains("\"name\":\"NoValues\""));
  }

  @Test
  void insertWithValueOutsideListFailsNamingColumnAndStoresNothing() throws IOException {
    assertFalse(execute(SAMPLE_IDS_QUERY).toString().contains("bad"));

    MolgenisException error =
        assertThrows(
            MolgenisException.class,
            () -> execute("mutation{insert(Sample:[{id:\"bad\",answer:\"maybe\"}]){message}}"));
    assertTrue(error.getMessage().contains("answer"), error.getMessage());
    assertTrue(error.getMessage().contains("maybe"), error.getMessage());

    assertFalse(execute(SAMPLE_IDS_QUERY).toString().contains("bad"));
  }

  @Test
  void insertWithAllowedValuesStoresThemAndReturnsThem() throws IOException {
    assertFalse(execute(SAMPLE_IDS_QUERY).toString().contains("good"));

    execute(
        "mutation{insert(Sample:[{id:\"good\",answer:\"unknown\",colors:[\"green\",\"blue\"]}]){message}}");

    String stored = execute("{Sample(filter:{id:{equals:\"good\"}}){answer,colors}}").toString();
    assertTrue(stored.contains("\"answer\":\"unknown\""), stored);
    assertTrue(stored.contains("\"colors\":[\"green\",\"blue\"]"), stored);
  }

  @Test
  void equalsFilterMatchesScalarEnumAndAnyElementOfEnumArray() throws IOException {
    String byAnswer = execute("{Sample(filter:{answer:{equals:\"yes\"}}){id}}").toString();
    assertTrue(byAnswer.contains("\"id\":\"s1\"") && !byAnswer.contains("\"id\":\"s2\""), byAnswer);

    String byColor = execute("{Sample(filter:{colors:{equals:[\"green\"]}}){id}}").toString();
    assertTrue(byColor.contains("\"id\":\"s2\"") && !byColor.contains("\"id\":\"s1\""), byColor);

    String byPickedColors =
        execute("{Sample(filter:{colors:{equals:[\"green\",\"blue\"]}}){id}}").toString();
    assertTrue(
        byPickedColors.contains("\"id\":\"s1\"") && byPickedColors.contains("\"id\":\"s2\""),
        byPickedColors);
  }

  @Test
  void emptyValuesListOnNonEnumColumnIsReturnedAsNoValues() throws IOException {
    execute(
        """
        mutation{change(tables:[{name:"PlainNote",columns:[
          {name:"id",key:1},{name:"note",values:[]}
        ]}]){message}}""");

    for (JsonNode table : execute(COLUMNS_QUERY).at("/_schema/tables")) {
      if ("PlainNote".equals(table.path("name").asText())) {
        for (JsonNode column : table.path("columns")) {
          if ("note".equals(column.path("name").asText())) {
            assertTrue(column.path("values").isNull() || column.path("values").isMissingNode());
            return;
          }
        }
      }
    }
    fail("column note not found");
  }

  private static JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
