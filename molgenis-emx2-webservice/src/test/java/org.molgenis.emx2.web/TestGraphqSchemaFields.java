package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.web.GraphqlApi.convertExecutionResultToJson;

public class TestGraphqSchemaFields {

  private static GraphQL grapql;
  private static final String schemaName = "TestGraphqlApiForDatabase";

  @BeforeClass
  public static void setup() {
    Database database = DatabaseFactory.getTestDatabase();
    Schema schema = database.createSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);
    grapql = GraphqlApi.graphqlForSchema(schema);
  }

  @Test
  public void testTableQueries() throws IOException {
    assertEquals("pooky", execute("{Pet{data{name}}}").at("/Pet/data/0/name").textValue());

    assertEquals(
        "spike",
        execute("{Pet(filter:{name:{equals:\"spike\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    assertEquals(
        "spike", execute("{Pet(search:\"spike\"){data{name}}}").at("/Pet/data/0/name").textValue());

    assertEquals(
        "spike", execute("{Pet{data(offset:1){name}}}").at("/Pet/data/0/name").textValue());

    assertEquals(1, execute("{Pet{data(limit:1){name}}}").at("/Pet/data").size());

    assertEquals(
        "pooky",
        execute("{Pet{data(orderby:{name:ASC}){name}}}").at("/Pet/data/0/name").textValue());

    assertEquals(
        "spike",
        execute("{Pet{data(orderby:{name:DESC}){name}}}").at("/Pet/data/0/name").textValue());

    assertEquals(
        "red",
        execute("{Pet(filter:{tags:{name:{equals:\"red\"}}}){data{name,tags{name}}}}")
            .at("/Pet/data/1/tags/0/name")
            .textValue());

    assertEquals(
        7,
        execute("{Order{data_agg{quantity{max}}}}").at("/Order/data_agg/quantity/max").intValue());
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper()
        .readTree(convertExecutionResultToJson(grapql.execute(query)))
        .get("data");
  }
}
