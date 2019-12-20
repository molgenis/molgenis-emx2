package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.molgenis.emx2.web.GraphqlApi.convertExecutionResultToJson;

public class TestGraphqSchemaFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqSchemaFields";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.createSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);
    grapql = GraphqlApi.createGraphqlForSchema(schema);
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
            .at("/Pet/data/0/tags/0/name")
            .textValue());

    // root agg
    assertEquals(
        7,
        execute("{Order{data_agg{quantity{max,min,sum,avg}}}}")
            .at("/Order/data_agg/quantity/max")
            .intValue());

    // nested agg
    assertEquals(
        15.7d,
        execute("{User{data{pets_agg{count,weight{max,min,sum,avg}}}}}")
            .at("/User/data/0/pets_agg/weight/max")
            .doubleValue(),
        0.0f);
  }

  @Test
  public void testMembersOperations() throws IOException {
    // list members
    int count = execute("{_meta{members{user}}}").at("/_meta/members").size();

    // add members
    execute("mutation{saveMeta(members:{user:\"blaat\", role:\"Manager\"}){message}}");
    assertEquals(count + 1, execute("{_meta{members{user}}}").at("/_meta/members").size());

    // remove members
    execute("mutation{deleteMeta(members:\"blaat\"){message}}");
    assertEquals(count, execute("{_meta{members{user}}}").at("/_meta/members").size());
  }

  @Test
  public void testLoginLogoutRegister() throws IOException {

    // todo: default user should be anonymous?
    assertNull(database.getActiveUser());

    execute("mutation{login(username:\"admin\"){message}}");
    assertEquals("admin", database.getActiveUser());

    // todo way to register witout elevated privileges
    assertFalse(database.hasUser("blaat"));
    execute("mutation{register(username:\"blaat\"){message}}");
    assertTrue(database.hasUser("blaat"));

    execute("mutation{logout{message}}");
    assertEquals("anonymous", database.getActiveUser());

    // can't unregister, is that a thing?

    // clear user back to supergodmode
    database.clearActiveUser();
  }

  @Test
  public void testTableAlterDropOperations() throws IOException {
    // simple meta
    assertEquals(5, execute("{_meta{tables{name}}}").at("/_meta/tables").size());

    // add table
    execute("mutation{saveMeta(tables:[{name:\"blaat\",columns:[{name:\"col1\"}]}]){message}}");
    assertEquals(6, execute("{_meta{tables{name}}}").at("/_meta/tables").size());

    // drop
    execute("mutation{deleteMeta(tables:\"blaat\"){message}}");
    assertEquals(5, execute("{_meta{tables{name}}}").at("/_meta/tables").size());
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper()
        .readTree(convertExecutionResultToJson(grapql.execute(query)))
        .get("data");
  }

  @Test
  public void saveAndDeleteRows() throws IOException {
    int count = execute("{Tag{data_agg{count}}}").at("/Tag/data_agg/count").intValue();
    // insert should increase count
    execute("mutation{save(Tag:{name:\"blaat\"}){message}}");
    assertEquals(count + 1, execute("{Tag{data_agg{count}}}").at("/Tag/data_agg/count").intValue());
    // delete
    execute("mutation{delete(Tag:{name:\"blaat\"}){message}}");
    assertEquals(count, execute("{Tag{data_agg{count}}}").at("/Tag/data_agg/count").intValue());
  }
}
