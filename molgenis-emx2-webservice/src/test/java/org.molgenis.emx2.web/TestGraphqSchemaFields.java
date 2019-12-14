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
  private static final String schemaName = "TestGraphqSchemaFields";

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
    // simple
    assertEquals("pooky", execute("{Pet{data{name}}}").at("/Pet/data/0/name").textValue());

    // simple ref
    assertEquals(
        "cat",
        execute("{Pet{data{name,category{name}}}}").at("/Pet/data/0/category/name").textValue());

    // equals text
    assertEquals(
        "spike",
        execute("{Pet(filter:{name:{equals:\"spike\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // not equals text
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{not_equals:\"spike\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // like text
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{like:\"oky\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());
    // not like text
    assertEquals(
        "spike",
        execute("{Pet(filter:{name:{not_like:\"oky\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // trigram
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{trigram_search:\"pook\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // textsearch
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{text_search:\"pook\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // equals int
    assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{equals:1}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // not equals int
    assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{not_equals:1}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // between int
    assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{between:[1,3]}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // not between int
    assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{not_between:[1,3]}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // equal bool
    assertEquals(
        "pooky",
        execute("{Order(filter:{complete:{equals:true}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // not equal bool
    assertEquals(
        "spike",
        execute("{Order(filter:{complete:{equals:false}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // search
    assertEquals(
        "spike",
        execute("{Pet(search:\"spike\"){data{name,category{name},tags{name}}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // offset
    assertEquals(
        "spike", execute("{Pet{data(offset:1){name}}}").at("/Pet/data/0/name").textValue());

    // limit
    assertEquals(1, execute("{Pet{data(limit:1){name}}}").at("/Pet/data").size());

    // orderby asc
    assertEquals(
        "pooky",
        execute("{Pet{data(orderby:{name:ASC}){name}}}").at("/Pet/data/0/name").textValue());

    // order by desc
    assertEquals(
        "spike",
        execute("{Pet{data(orderby:{name:DESC}){name}}}").at("/Pet/data/0/name").textValue());

    // filter nested
    assertEquals(
        "red",
        execute("{Pet(filter:{tags:{name:{equals:\"red\"}}}){data{name,tags{name}}}}")
            .at("/Pet/data/1/tags/0/name")
            .textValue());

    // root agg
    assertEquals(
        7,
        execute("{Order{data_agg{quantity{max,min,sum,avg}}}}")
            .at("/Order/data_agg/quantity/max")
            .intValue());

    // nested agg
    assertEquals(
        2,
        execute("{Order{data{pet{tags_agg{count}}}}}")
            .at("/Order/data/1/pet/tags_agg/count")
            .intValue());
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper()
        .readTree(convertExecutionResultToJson(grapql.execute(query)))
        .get("data");
  }
}
