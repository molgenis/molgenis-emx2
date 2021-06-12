package org.molgenis.emx2.graphql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqSchemaFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqlSchemaFields";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);
    grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
  }

  @Test
  public void testSession() throws IOException {
    try {
      TestCase.assertEquals(0, execute("{_session{email,roles}}").at("/_session/roles").size());

      // first become other user
      database.setActiveUser("shopmanager");

      TestCase.assertEquals(
          "Manager", execute("{_session{email,roles}}").at("/_session/roles/2").textValue());
    } finally {
      database.clearActiveUser();
    }
  }

  @Test
  public void testSchemaSettings() throws IOException {
    // add value
    execute("mutation{change(settings:{key:\"test\",value:\"testval\"}){message}}");

    assertEquals("testval", execute("{_settings{key,value}}").at("/_settings/0/value").textValue());

    // remove value
    execute("mutation{drop(settings:{key:\"test\"}){message}}");

    assertEquals(0, execute("{_settings{key,value}}").at("/_settings").size());
  }

  @Test
  public void testTableSettings() throws IOException {
    // add value
    execute(
        "mutation{change(tables:[{name:\"Pet\",settings:{key:\"test\",value:\"testval\"}}]){message}}");

    assertEquals(
        "testval",
        execute("{_schema{tables{settings{key,value}}}}")
            .at("/_schema/tables/2/settings/0/value")
            .textValue());

    // remove value
    execute("mutation{drop(settings:[{table:\"Pet\", key:\"test\"}]){message}}");

    assertEquals(
        0,
        execute("{_schema{tables{settings{key,value}}}}").at("/_schema/tables/2/settings").size());

    // update via the shortcut
    execute(
        "mutation{change(settings:[{table:\"Pet\",key:\"test2\",value:\"testval2\"}]){message}}");

    assertEquals(
        "testval2",
        execute("{_schema{tables{settings{key,value}}}}")
            .at("/_schema/tables/2/settings/0/value")
            .textValue());

    assertEquals(
        "test2",
        execute("{_schema{tables{settings{key,value}}}}")
            .at("/_schema/tables/2/settings/0/key")
            .textValue());
  }

  @Test
  public void testTableQueries() throws IOException {
    // simple
    TestCase.assertEquals("pooky", execute("{Pet{name}}").at("/Pet/0/name").textValue());

    TestCase.assertEquals(
        "pooky", execute("{Pet{name}Pet_agg{count}}").at("/Pet/0/name").textValue());

    // simple ref
    JsonNode result = execute("{Pet{name,category{name}}}");
    TestCase.assertEquals("cat", result.at("/Pet/0/category/name").textValue());

    // equals text
    TestCase.assertEquals(
        "spike",
        execute("{Pet(filter:{name:{equals:\"spike\"}}){name}}").at("/Pet/0/name").textValue());

    // not equals text
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{not_equals:\"spike\"}}){name}}").at("/Pet/0/name").textValue());

    // like text
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{like:\"oky\"}}){name}}").at("/Pet/0/name").textValue());
    // not like text
    TestCase.assertEquals(
        "spike",
        execute("{Pet(filter:{name:{not_like:\"oky\"}}){name}}").at("/Pet/0/name").textValue());

    // trigram
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{trigram_search:\"pook\"}}){name}}")
            .at("/Pet/0/name")
            .textValue());

    // textsearch
    TestCase.assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{text_search:\"pook\"}}){name}}").at("/Pet/0/name").textValue());

    // equals int
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{equals:1}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // not equals int
    TestCase.assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{not_equals:1}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // between int
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{between:[1,3]}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // between int one sided
    TestCase.assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{between:[3,null]}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // between int one sided
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{not_between:[null,3]}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // equal bool
    TestCase.assertEquals(
        "pooky",
        execute("{Order(filter:{complete:{equals:true}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // not equal bool
    TestCase.assertEquals(
        "spike",
        execute("{Order(filter:{complete:{equals:false}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // search
    TestCase.assertEquals(
        "spike",
        execute("{Pet(search:\"spike\"){name,category{name},tags{name}}}")
            .at("/Pet/0/name")
            .textValue());

    // offset
    TestCase.assertEquals(
        "spike", execute("{Pet(offset:1,orderby:{name:ASC}){name}}").at("/Pet/0/name").textValue());

    // limit
    TestCase.assertEquals(1, execute("{Pet(limit:1){name}}").at("/Pet").size());

    // orderby asc
    TestCase.assertEquals(
        "pooky", execute("{Pet(orderby:{name:ASC}){name}}").at("/Pet/0/name").textValue());

    // order by desc
    TestCase.assertEquals(
        "spike", execute("{Pet(orderby:{name:DESC}){name}}").at("/Pet/0/name").textValue());

    // filter nested
    TestCase.assertEquals(
        "red",
        execute("{Pet(filter:{tags:{name:{equals:\"red\"}}}){name,tags{name}}}")
            .at("/Pet/0/tags/0/name")
            .textValue());

    // or
    TestCase.assertEquals(
        2,
        execute("{Pet(filter:{_or:[{name:{equals:\"pooky\"}},{name:{equals:\"spike\"}}]}){name}}")
            .at("/Pet")
            .size());

    // or nested
    TestCase.assertEquals(
        2,
        execute(
                "{Pet(filter:{_or:[{name:{equals:\"pooky\"}},{tags:{_or:[{name:{equals:\"green\"}}]}}]}){name}}")
            .at("/Pet")
            .size());

    // or nested
    TestCase.assertEquals(
        2,
        execute(
                "{Pet_agg(filter:{_or:[{name:{equals:\"pooky\"}},{tags:{_or:[{name:{equals:\"green\"}}]}}]}){count}}")
            .at("/Pet_agg/count")
            .intValue());

    // root agg
    TestCase.assertEquals(
        7,
        execute("{Order_agg{quantity{max,min,sum,avg}}}").at("/Order_agg/quantity/max").intValue());

    // nested agg
    TestCase.assertEquals(
        15.7d,
        execute("{User{pets_agg{count,weight{max,min,sum,avg}}}}")
            .at("/User/0/pets_agg/weight/max")
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
    execute("mutation{change(members:{email:\"blaat\", role:\"Manager\"}){message}}");
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
    execute(
        "mutation{change(tables:[{name:\"blaat\",columns:[{name:\"col1\", key:1}]}]){message}}");

    JsonNode node = execute("{_schema{tables{name,columns{name,key}}}}");

    TestCase.assertEquals(
        1,
        execute("{_schema{tables{name,columns{name,key}}}}")
            .at("/_schema/tables/0/columns/0/key")
            .intValue());
    TestCase.assertEquals(6, execute("{_schema{tables{name}}}").at("/_schema/tables").size());

    // drop
    execute("mutation{drop(tables:\"blaat\"){message}}");
    TestCase.assertEquals(5, execute("{_schema{tables{name}}}").at("/_schema/tables").size());
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }

  @Test
  public void saveAndDeleteRows() throws IOException {
    int count = execute("{Tag_agg{count}}").at("/Tag_agg/count").intValue();
    // insert should increase count
    execute("mutation{insert(Tag:{name:\"blaat\"}){message}}");
    TestCase.assertEquals(count + 1, execute("{Tag_agg{count}}").at("/Tag_agg/count").intValue());
    // delete
    execute("mutation{delete(Tag:{name:\"blaat\"}){message}}");
    TestCase.assertEquals(count, execute("{Tag_agg{count}}").at("/Tag_agg/count").intValue());
  }

  @Test
  public void testAddAlterDropColumn() throws IOException {
    execute("mutation{change(columns:{table:\"Pet\",name:\"test\"}){message}}");
    TestCase.assertNotNull(
        database.getSchema(schemaName).getTable("Pet").getMetadata().getColumn("test"));
    execute(
        "mutation{change(columns:{table:\"Pet\", oldName:\"test\",name:\"test2\", key:3, columnType:\"INT\"}){message}}");

    database.clearCache(); // cannot know here, server clears caches

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

    database.clearCache(); // cannot know here, server clears caches

    assertNull(database.getSchema(schemaName).getTable("Pet").getMetadata().getColumn("test2"));

    execute(
        "mutation{change(columns:{table:\"Pet\", name:\"test2\", columnType:\"STRING\", columnFormat:\"Hyperlink\", visible:\"blaat\"}){message}}");
    database.clearCache(); // cannot know here, server clears caches
    assertEquals(
        "Hyperlink",
        database
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2")
            .getColumnFormat());
    assertEquals(
        "blaat",
        database
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2")
            .getVisible());
    execute("mutation{drop(columns:[{table:\"Pet\", column:\"test2\"}]){message}}");
  }

  @Test
  public void testFileType() {}
}
