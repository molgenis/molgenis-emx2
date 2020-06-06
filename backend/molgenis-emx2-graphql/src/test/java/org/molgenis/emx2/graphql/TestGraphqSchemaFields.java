package org.molgenis.emx2.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.createGraphqlForSchema;

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
    grapql = createGraphqlForSchema(schema);
  }

  @Test
  public void testTableQueries() throws IOException {
    // simple
    TestCase.assertEquals("pooky", execute("{Pet{data{name}}}").at("/Pet/data/0/name").textValue());

    // simple ref
    TestCase.assertEquals(
        "cat",
        execute("{Pet{data{name,category{name}}}}").at("/Pet/data/0/category/name").textValue());

    // equals text
    TestCase.assertEquals(
        "spike",
        execute("{Pet(filter:{name:{equals:\"spike\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // not equals text
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{not_equals:\"spike\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // like text
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{like:\"oky\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());
    // not like text
    TestCase.assertEquals(
        "spike",
        execute("{Pet(filter:{name:{not_like:\"oky\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // trigram
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{trigram_search:\"pook\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // textsearch
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{text_search:\"pook\"}}){data{name}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // equals int
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{equals:1}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // not equals int
    TestCase.assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{not_equals:1}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // between int
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{between:[1,3]}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // between int one sided
    TestCase.assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{between:[3,null]}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // between int one sided
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{not_between:[null,3]}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // equal bool
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{complete:{equals:true}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // not equal bool
    TestCase.assertEquals(
        "spike",
        execute("{Order(filter:{complete:{equals:false}}){data{quantity,pet{name}}}}")
            .at("/Order/data/0/pet/name")
            .textValue());

    // search
    TestCase.assertEquals(
        "spike",
        execute("{Pet(search:\"spike\"){data{name,category{name},tags{name}}}}")
            .at("/Pet/data/0/name")
            .textValue());

    // offset
    TestCase.assertEquals(
        "spike", execute("{Pet{data(offset:1){name}}}").at("/Pet/data/0/name").textValue());

    // limit
    TestCase.assertEquals(1, execute("{Pet{data(limit:1){name}}}").at("/Pet/data").size());

    // orderby asc
    TestCase.assertEquals(
        "pooky",
        execute("{Pet{data(orderby:{name:ASC}){name}}}").at("/Pet/data/0/name").textValue());

    // order by desc
    TestCase.assertEquals(
        "spike",
        execute("{Pet{data(orderby:{name:DESC}){name}}}").at("/Pet/data/0/name").textValue());

    // filter nested
    TestCase.assertEquals(
        "red",
        execute("{Pet(filter:{tags:{name:{equals:\"red\"}}}){data{name,tags{name}}}}")
            .at("/Pet/data/0/tags/0/name")
            .textValue());

    // root agg
    TestCase.assertEquals(
        7,
        execute("{Order{data_agg{quantity{max,min,sum,avg}}}}")
            .at("/Order/data_agg/quantity/max")
            .intValue());

    // nested agg
    TestCase.assertEquals(
        15.7d,
        execute("{User{data{pets_agg{count,weight{max,min,sum,avg}}}}}")
            .at("/User/data/0/pets_agg/weight/max")
            .doubleValue(),
        0.0f);
  }

  @Test
  public void testSchemaQueries() throws IOException {
    TestCase.assertEquals(schemaName, execute("{_schema{name}}").at("/_schema/name").textValue());
  }

  @Test
  public void testMembersOperations() throws IOException {

    // list members
    int count = execute("{_schema{members{email}}}").at("/_schema/members").size();

    // add members
    execute("mutation{create(members:{email:\"blaat\", role:\"Manager\"}){message}}");
    TestCase.assertEquals(
        count + 1, execute("{_schema{members{email}}}").at("/_schema/members").size());

    // remove members
    execute("mutation{drop(members:\"blaat\"){message}}");
    TestCase.assertEquals(
        count, execute("{_schema{members{email}}}").at("/_schema/members").size());
  }

  @Test
  public void testTableAlterDropOperations() throws IOException {
    // simple meta
    TestCase.assertEquals(5, execute("{_schema{tables{name}}}").at("/_schema/tables").size());

    // add table
    execute("mutation{create(tables:[{name:\"blaat\",columns:[{name:\"col1\"}]}]){message}}");
    TestCase.assertEquals(6, execute("{_schema{tables{name}}}").at("/_schema/tables").size());

    // drop
    execute("mutation{drop(tables:\"blaat\"){message}}");
    TestCase.assertEquals(5, execute("{_schema{tables{name}}}").at("/_schema/tables").size());
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    return new ObjectMapper().readTree(result).get("data");
  }

  @Test
  public void saveAndDeleteRows() throws IOException {
    int count = execute("{Tag{data_agg{count}}}").at("/Tag/data_agg/count").intValue();
    // insert should increase count
    execute("mutation{insert(Tag:{name:\"blaat\"}){message}}");
    TestCase.assertEquals(
        count + 1, execute("{Tag{data_agg{count}}}").at("/Tag/data_agg/count").intValue());
    // delete
    execute("mutation{delete(Tag:{name:\"blaat\"}){message}}");
    TestCase.assertEquals(
        count, execute("{Tag{data_agg{count}}}").at("/Tag/data_agg/count").intValue());
  }

  @Test
  public void testAddAlterDropColumn() throws IOException {
    execute("mutation{create(columns:{table:\"Pet\",name:\"test\", nullable:true}){message}}");
    TestCase.assertNotNull(
        database.getSchema(schemaName).getTable("Pet").getMetadata().getColumn("test"));

    execute(
        "mutation{alter(columns:{table:\"Pet\", name:\"test\", definition:{name:\"test2\", nullable:true, columnType:\"INT\"}}){message}}");
    assertNull(database.getSchema(schemaName).getTable("Pet").getMetadata().getColumn("test"));
    TestCase.assertEquals(
        ColumnType.INT,
        database
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2")
            .getColumnType());

    execute("mutation{drop(columns:[{table:\"Pet\", column:\"test2\"}]){message}}");
    assertNull(database.getSchema(schemaName).getTable("Pet").getMetadata().getColumn("test2"));
  }
}
