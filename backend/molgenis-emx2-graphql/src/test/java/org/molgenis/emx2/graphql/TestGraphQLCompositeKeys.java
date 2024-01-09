package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphQLCompositeKeys {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = TestGraphQLCompositeKeys.class.getSimpleName();

  // need ref
  // need mref
  // need ref_array

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(schemaName);
    grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
  }

  @Test
  public void testRefArray() throws IOException {
    // create schema (best edit this in graphql editor)
    execute(
        "mutation {\n"
            + "  change(\n"
            + "    tables: [\n"
            + "      { name: \"TargetTable\"\n"
            + "        columns: [\n"
            + "          { name: \"firstName\", key: 1 }\n"
            + "          { name: \"lastName\", key: 1 }\n"
            + "        ]\n"
            + "      }\n"
            + "      { name: \"RefTable\"\n"
            + "        columns: [\n"
            + "          { name: \"id1\", columnType: \"INT\", key: 1 }\n"
            + "          { name: \"id2\", key: 1}\n"
            + "          { name: \"ref\", columnType: \"REF_ARRAY\", refTableName: \"TargetTable\" }\n"
            + "        ]\n"
            + "      }\n"
            + "    ]\n"
            + "  ) {\n"
            + "    message\n"
            + "  }\n"
            + "}");

    // add refBack (TODO unfortunately cannot yet do that in one call)
    execute(
        "mutation {change(columns: [{table: \"TargetTable\" name: \"refBacks\" columnType:"
            + " \"REFBACK\" refTableName: \"RefTable\" refBackName: \"ref\"}]) {message}}");

    // have to reload graphql
    grapql =
        new GraphqlApiFactory()
            .createGraphqlForSchema(
                database.getSchema(TestGraphQLCompositeKeys.class.getSimpleName()));

    // insert some data, enough to check if foreign keys are joined correctly
    execute(
        "mutation{insert(TargetTable:["
            + "\n{firstName:\"Donald\",lastName:\"Duck\"}"
            + "\n{firstName:\"Katrien\",lastName:\"Duck\"}"
            + "\n{firstName:\"Katrien\",lastName:\"Mouse\"}"
            + "\n{firstName:\"Donald\",lastName:\"Mouse\"}]){message}}");
    execute(
        "mutation{insert(RefTable:["
            + "\n{id1:1,id2:\"a\", ref:[{firstName:\"Katrien\",lastName:\"Mouse\"},{firstName:\"Donald\",lastName:\"Duck\"}]}"
            + "\n{id1:2,id2:\"a\", ref:[{firstName:\"Katrien\",lastName:\"Duck\"},{firstName:\"Donald\",lastName:\"Mouse\"}]}"
            + "\n{id1:3,id2:\"a\", ref:[{firstName:\"Katrien\",lastName:\"Duck\"}]}"
            + "]){message}}");

    // test query
    JsonNode result = execute("{RefTable{id1,id2,ref{firstName,lastName}}}");
    System.out.println(result.toPrettyString());

    assertEquals(2, result.at("/RefTable/0/ref").size());
    // order is determined by TargetTable unfortunately
    assertEquals("Katrien", result.at("/RefTable/0/ref/1/firstName").asText());
    assertEquals("Mouse", result.at("/RefTable/0/ref/1/lastName").asText());
    assertEquals("Donald", result.at("/RefTable/0/ref/0/firstName").asText());
    assertEquals("Duck", result.at("/RefTable/0/ref/0/lastName").asText());

    // update via refbBack, only id1=3,id2=1 should now refer to Donald,Duck
    execute(
        "mutation{update(TargetTable:[{firstName:\"Donald\",lastName:\"Duck\", refBacks:[{id1:3,id2:\"a\"}]}]){message}}");

    String filter = "(filter:{ref:{equals:{firstName:\"Donald\",lastName:\"Duck\"}}})";
    result =
        execute(
            "{RefTable"
                + filter
                + "{id1,id2,ref{firstName,lastName}}RefTable_agg"
                + filter
                + "{count}}");
    System.out.println(result.toPrettyString());
    assertEquals(1, result.at("/RefTable_agg/count").asInt());
    assertEquals("Donald", result.at("/RefTable/0/ref/1/firstName").asText());
    assertEquals("Duck", result.at("/RefTable/0/ref/1/lastName").asText());

    result =
        execute(
            "{TargetTable(filter:{equals:{firstName:\"Donald\",lastName:\"Duck\"}}){firstName,lastName}}");
    System.out.println(result.toPrettyString());
    assertEquals("Donald", result.at("/TargetTable/0/firstName").asText());
    assertEquals("Duck", result.at("/TargetTable/0/lastName").asText());

    String filter_agg =
        "filter:{equals:[{firstName:\"Donald\",lastName:\"Duck\"},{firstName:\"Katrien\",lastName:\"Mouse\"}]}";
    filter = "orderby:{firstName:ASC}" + filter_agg;
    result =
        execute(
            "{TargetTable("
                + filter
                + "){firstName,lastName},TargetTable_agg("
                + filter_agg
                + "){count}}");
    System.out.println(result.toPrettyString());
    assertEquals("Donald", result.at("/TargetTable/0/firstName").asText());
    assertEquals("Duck", result.at("/TargetTable/0/lastName").asText());
    assertEquals("Katrien", result.at("/TargetTable/1/firstName").asText());
    assertEquals("Mouse", result.at("/TargetTable/1/lastName").asText());
    assertEquals(2, result.at("/TargetTable_agg/count").asInt());

    filter = "(filter:{ref:{equals:[{firstName:\"Donald\",lastName:\"Duck\"}]}})";
    result =
        execute(
            "{RefTable"
                + filter
                + "{id1,id2,ref{firstName,lastName}}RefTable_agg"
                + filter
                + "{count}}");
    System.out.println(result.toPrettyString());
    assertEquals(1, result.at("/RefTable_agg/count").asInt());
  }

  //  @Test
  //  public void testMref() throws IOException {
  //    // create schema (best edit this in graphql editor)
  //    execute(
  //        "mutation {\n"
  //            + "  change(\n"
  //            + "    tables: [\n"
  //            + "      { name: \"TargetTable2\", columns: [{ name: \"firstName\", key: 1 },{ name:
  // \"lastName\", key: 1 }] }\n"
  //            + "      {\n"
  //            + "        name: \"RefTable2\"\n"
  //            + "        columns: [\n"
  //            + "          { name: \"id\", key: 1 }\n"
  //            + "          { name: \"ref\", columnType: \"MREF\", refTable: \"TargetTable2\"}\n"
  //            + "        ]\n"
  //            + "      }\n"
  //            + "    ]\n"
  //            + "  ) {\n"
  //            + "    message\n"
  //            + "  }\n"
  //            + "}");
  //
  //    // have to reload graphql
  //    grapql =
  //        new GraphqlApiFactory()
  //            .createGraphqlForSchema(
  //                database.getSchema(TestGraphQLCompositeKeys.class.getSimpleName()));
  //
  //    // insert some data, enough to check if foreign keys are joined correctly
  //    execute(
  //        "mutation{insert(TargetTable2:["
  //            + "\n{firstName:\"Donald\",lastName:\"Duck\"}"
  //            + "\n{firstName:\"Katrien\",lastName:\"Duck\"}"
  //            + "\n{firstName:\"Katrien\",lastName:\"Mouse\"}"
  //            + "\n{firstName:\"Donald\",lastName:\"Mouse\"}"
  //            + "\n], RefTable2:["
  //            + "\n{id:\"1\",
  // ref:[{firstName:\"Katrien\",lastName:\"Mouse\"},{firstName:\"Donald\",lastName:\"Duck\"}]}"
  //            + "\n{id:\"2\",
  // ref:[{firstName:\"Katrien\",lastName:\"Duck\"},{firstName:\"Donald\",lastName:\"Mouse\"}]}"
  //            + "\n{id:\"3\", ref:[{firstName:\"Katrien\",lastName:\"Duck\"}]}"
  //            + "]){message}}");
  //
  //    // test query
  //    JsonNode result = execute("{RefTable2{id,ref{firstName,lastName}}}");
  //    System.out.println(result.toPrettyString());
  //
  //    assertEquals(2, result.at("/RefTable2/0/ref").size());
  //    // order is determined by TargetTable unfortunately
  //    assertEquals("Katrien", result.at("/RefTable2/0/ref/1/firstName").asText());
  //    assertEquals("Mouse", result.at("/RefTable2/0/ref/1/lastName").asText());
  //    assertEquals("Donald", result.at("/RefTable2/0/ref/0/firstName").asText());
  //    assertEquals("Duck", result.at("/RefTable2/0/ref/0/lastName").asText());
  //  }

  private static JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    } else {
      if (node.get("data") != null && node.get("data").get("message") != null) {
        System.out.println("MUTATION MESSAGE: " + node.get("data").get("message").asText());
      }
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
