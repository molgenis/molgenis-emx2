package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;
import static org.molgenis.emx2.utils.TypeUtils.convertToPascalCase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.Task;

public class TestGraphqlSchemaFields {

  private static String schemaName = TestGraphqlSchemaFields.class.getSimpleName();
  private static Schema schema;
  private static GraphqlSession session;

  @BeforeAll
  public static void setup() {
    Database database = new SqlDatabase(ADMIN_USER);
    final String shopviewer = "shopviewer";
    final String shopmanager = "shopmanager";
    final String shopowner = "shopowner";
    final String costumer = "costumer";

    // initialize users
    database.setUserPassword(shopmanager, shopmanager);
    database.setUserPassword(shopviewer, shopviewer);
    database.setUserPassword(shopowner, shopowner);
    database.setUserPassword(costumer, costumer);

    schema = database.dropCreateSchema(schemaName);
    schema.addMember(shopmanager, "Manager");
    schema.addMember(shopviewer, "Viewer");
    schema.addMember(shopowner, "Owner");
    schema.addMember(costumer, "Range");
    DataModels.getImportTask(schema, PET_STORE.name(), true).run();
    schema = database.getSchema(schemaName);

    session = new GraphqlSession(ADMIN_USER);
  }

  @Test
  void testMatchInParentsAndChildren() throws IOException {
    String result =
        execute("{Tag(filter:{_match_any_including_children:[\"colors\"]}){name}}").toString();
    assertTrue(result.contains("red"));
    assertFalse(result.contains("mammals"));

    result = execute("{Tag(filter:{_match_any_including_parents:[\"red\"]}){name}}").toString();
    assertTrue(result.contains("colors"));
    assertFalse(result.contains("mammals"));

    result = execute("{Tag(filter:{_search_including_parents:[\"re\"]}){name}}").toString();
    assertTrue(result.contains("colors"));
    assertTrue(result.contains("green"));
    assertTrue(result.contains("red"));
    assertFalse(result.contains("mammals"));

    // just to check syntax works, the real tests live in sql
    result =
        execute("{Pet(filter:{tags:{_match_any_including_children:\"colors\"}}){name}}").toString();
    assertTrue(result.contains("tom"));
    assertFalse(result.contains("pooky")); // poor pooky has no color

    result =
        execute("{Pet(filter:{tags:{_match_any_including_parents:\"red\"}}){name}}").toString();
    assertTrue(result.contains("tom"));
    assertFalse(result.contains("pooky")); // poor pooky has no color

    result = execute("{Pet(filter:{tags:{_search_including_parents:\"re\"}}){name}}").toString();
    assertTrue(result.contains("tom"));
    assertFalse(result.contains("pooky")); // poor pooky has no color

    result =
        execute("{Pet(filter:{tags:{_match_any_including_children:[\"green\",\"blue\"]}}){name}}")
            .toString();
    assertTrue(result.contains("jerry"));
    assertTrue(result.contains("spike"));
    assertFalse(result.contains("tom")); // tom is red

    result =
        execute("{Pet(filter:{tags:{_match_any_including_parents:[\"green\",\"blue\"]}}){name}}")
            .toString();
    assertTrue(result.contains("jerry"));
    assertTrue(result.contains("spike"));
    assertFalse(result.contains("tom")); // tom is red

    result = execute("{Pet(filter:{tags:{_match_path:[\"green\",\"blue\"]}}){name}}").toString();
    assertTrue(result.contains("jerry"));
    assertTrue(result.contains("spike"));
    assertFalse(result.contains("tom")); // tom is red
  }

  @Test
  void testNullAndNotNull() throws IOException {
    // ref
    String result = execute("{Pet(filter:{tags:{_is_null:true}}){name}}").toString();
    assertTrue(result.contains("pooky"));
    assertFalse(result.contains("tom"));

    result = execute("{Pet(filter:{tags:{_is_null:false}}){name}}").toString();
    assertTrue(result.contains("tom"));
    assertFalse(result.contains("pooky"));

    // refback+ref
    result = execute("{Pet(filter:{orders:{_is_null:true}}){name}}").toString();
    assertTrue(result.contains("tom"));
    assertTrue(result.contains("sylvester"));
    assertFalse(result.contains("pooky"));

    result = execute("{Pet(filter:{orders:{_is_null:false}}){name}}").toString();
    assertTrue(result.contains("spike"));
    assertTrue(result.contains("pooky"));
    assertFalse(result.contains("tom"));

    // ref_array
    result = execute("{User(filter:{pets:{_is_null:true}}){username}}").toString();
    assertFalse(result.contains("bofke"));

    result = execute("{User(filter:{pets:{_is_null:false}}){username}}").toString();
    assertTrue(result.contains("bofke"));
  }

  @Test
  public void testSession() throws IOException {
    try {
      session.setSessionUser(ANONYMOUS);
      assertEquals(5, execute("{_session{email,roles}}").at("/_session/roles").size());
      execute("mutation { signin(email: \"shopmanager\",password:\"shopmanager\") {message}}");
      session.setSessionUser(ADMIN_USER);
      assertTrue(execute("{_session{email,roles}}").toString().contains("Manager"));
    } finally {
      session.setSessionUser(ADMIN_USER);
    }
  }

  @Test
  public void testSchemaSettings() throws IOException {
    // add value
    execute("mutation{change(settings:{key:\"test\",value:\"testval\"}){message}}");

    assertEquals("testval", execute("{_settings{key,value}}").at("/_settings/1/value").textValue());

    // remove value
    execute("mutation{drop(settings:{key:\"test\"}){message}}");
  }

  @Test
  public void testFetchSchemaSettingsByKey() throws IOException {
    // add value
    execute("mutation{change(settings:{key:\"setA\",value:\"valA\"}){message}}");
    execute("mutation{change(settings:{key:\"setB\",value:\"valB\"}){message}}");

    // fetch by key
    assertEquals(2, execute("{_settings(keys: [\"setB\"]){key,value}}").at("/_settings").size());
    assertEquals(
        "valB",
        execute("{_settings(keys: [\"setB\"]){key,value}}").at("/_settings/0/value").textValue());

    // return all without key
    assertEquals(4, execute("{_settings{key,value}}").at("/_settings").size());

    // remove value
    execute("mutation{drop(settings:{key:\"setA\"}){message}}");
    execute("mutation{drop(settings:{key:\"setB\"}){message}}");
  }

  @Test
  public void testFetchSchemaSettingsForPages() throws IOException {
    // add value
    execute("mutation{change(settings:{key:\"page.mypage\",value:\"page value\"}){message}}");

    // include all pages
    assertEquals(
        "page value",
        execute("{_settings(keys: [\"page.\"]){key,value}}").at("/_settings/0/value").textValue());

    // remove value
    execute("mutation{drop(settings:{key:\"page.mypage\"}){message}}");
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
    execute("mutation{drop(settings:[{tableId:\"Pet\", key:\"test\"}]){message}}");

    assertEquals(0, schema.getTable("Pet").getMetadata().getSettings().size());
    assertEquals(
        0,
        execute("{_schema{tables{settings{key,value}}}}").at("/_schema/tables/2/settings").size());

    // or using tableId
    // add value
    execute("mutation{change(settings:[{tableId:\"Pet\",key:\"test\",value:\"blaat\"}]){message}}");

    assertEquals(
        "blaat",
        execute("{_schema{tables{settings{key,value}}}}")
            .at("/_schema/tables/2/settings/0/value")
            .textValue());

    // remove value
    execute("mutation{drop(settings:[{tableId:\"Pet\", key:\"test\"}]){message}}");

    assertEquals(0, schema.getTable("Pet").getMetadata().getSettings().size());
    assertEquals(
        0,
        execute("{_schema{tables{settings{key,value}}}}").at("/_schema/tables/2/settings").size());
  }

  @Tag("windowsFail")
  @Test
  public void testTableQueries() throws IOException {
    // simple
    assertEquals("pooky", execute("{Pet{name}}").at("/Pet/0/name").textValue());

    assertEquals("pooky", execute("{Pet{name}Pet_agg{count}}").at("/Pet/0/name").textValue());

    // simple ref
    JsonNode result = execute("{Pet{name,category{name}}}");
    assertEquals("cat", result.at("/Pet/0/category/name").textValue());

    // equals text
    assertEquals(
        "spike",
        execute("{Pet(filter:{name:{equals:\"spike\"}}){name}}").at("/Pet/0/name").textValue());

    // not equals text
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{not_equals:\"spike\"}}){name}}").at("/Pet/0/name").textValue());

    // like text
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{like:\"oky\"}}){name}}").at("/Pet/0/name").textValue());
    // not like text
    assertEquals(
        "spike",
        execute("{Pet(filter:{name:{not_like:\"oky\"}}){name}}").at("/Pet/0/name").textValue());

    // trigram
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{trigram_search:\"pook\"}}){name}}")
            .at("/Pet/0/name")
            .textValue());

    // textsearch
    assertEquals(
        "pooky",
        execute("{Pet(filter:{name:{text_search:\"pook\"}}){name}}").at("/Pet/0/name").textValue());

    // equals int
    assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{equals:1}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // not equals int
    assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{not_equals:1}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // between int
    assertEquals(
        "pooky",
        execute("{Order(filter:{quantity:{between:[1,3]}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // between int one sided
    assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{between:[3,null]}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // between int one sided
    assertEquals(
        "spike",
        execute("{Order(filter:{quantity:{not_between:[null,3]}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // equal bool
    assertEquals(
        "pooky",
        execute("{Order(filter:{complete:{equals:true}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // not equal bool
    assertEquals(
        "spike",
        execute("{Order(filter:{complete:{equals:false}}){quantity,pet{name}}}")
            .at("/Order/0/pet/name")
            .textValue());

    // search
    assertEquals(
        "spike",
        execute("{Pet(search:\"spike\"){name,category{name},tags{name}}}")
            .at("/Pet/0/name")
            .textValue());

    // or you can also search via filter
    assertEquals(
        "pooky",
        execute("{Pet(filter:{_search:\"pooky\"}){name,category{name},tags{name}}}")
            .at("/Pet/0/name")
            .textValue());

    // offset
    assertEquals(
        "jerry", execute("{Pet(offset:1,orderby:{name:ASC}){name}}").at("/Pet/0/name").textValue());

    // limit
    assertEquals(1, execute("{Pet(limit:1){name}}").at("/Pet").size());

    // orderby asc
    assertEquals(
        "fire ant", execute("{Pet(orderby:{name:ASC}){name}}").at("/Pet/0/name").textValue());

    // order by desc
    assertEquals(
        "tweety", execute("{Pet(orderby:{name:DESC}){name}}").at("/Pet/0/name").textValue());

    // order by on non-root column
    assertEquals(
        "delivered",
        execute("{Pet {orders(orderby: {orderId: ASC}) {status}}}")
            .at("/Pet/0/orders/0/status")
            .textValue());

    // filter nested
    assertEquals(
        "red",
        execute("{Pet(filter:{tags:{name:{equals:\"red\"}}}){name,tags{name}}}")
            .at("/Pet/0/tags/0/name")
            .textValue());

    // or
    assertEquals(
        2,
        execute("{Pet(filter:{_or:[{name:{equals:\"pooky\"}},{name:{equals:\"spike\"}}]}){name}}")
            .at("/Pet")
            .size());

    // or nested
    assertEquals(
        4,
        execute(
                "{Pet(filter:{_or:[{name:{equals:\"pooky\"}},{tags:{_or:[{name:{equals:\"green\"}}]}}]}){name}}")
            .at("/Pet")
            .size());

    // or nested
    assertEquals(
        4,
        execute(
                "{Pet_agg(filter:{_or:[{name:{equals:\"pooky\"}},{tags:{_or:[{name:{equals:\"green\"}}]}}]}){count}}")
            .at("/Pet_agg/count")
            .intValue());

    // root agg
    assertEquals(
        7,
        execute("{Order_agg{max{quantity},min{quantity},_sum{quantity},avg{quantity}}}")
            .at("/Order_agg/max/quantity")
            .intValue());

    // nested agg
    assertEquals(
        15.7d,
        execute("{User{pets_agg{count,max{weight},min{weight},_sum{weight},avg{weight}}}}")
            .at("/User/0/pets_agg/max/weight")
            .doubleValue(),
        0.0f);

    // nested agg with filter
    assertEquals(
        1,
        execute("{User{pets_agg(filter:{name:{equals:\"pooky\"}}){count}}}")
            .at("/User/0/pets_agg/count")
            .intValue());

    // subfilters
    result =
        execute(
            "{Pet(filter:{name:{equals:\"spike\"}}){name,tags(filter:{name:{equals:\"green\"}}){name}}}");
    assertEquals("spike", result.at("/Pet/0/name").textValue());
    assertEquals("green", result.at("/Pet/0/tags/0/name").textValue());
    assertEquals(1, result.at("/Pet/0/tags").size());

    // nested orderby should give reasonable error
    try {
      execute("{Pet(filter:{name:{equals:\"spike\"}}){name,tags(orderby:{blaat:ASC}){name}}}");
      fail("should fail");
    } catch (MolgenisException e) {
      assertTrue(
          e.getMessage()
              .contains(
                  "Validation error (WrongType@[Pet/tags]) : argument 'orderby' with value 'ObjectValue{objectFields=[ObjectField{name='blaat', value=EnumValue{name='ASC'}}]}' contains a field not in 'Tagorderby': 'blaat'"));
    }
  }

  @Test
  public void testGroupBy() throws IOException {
    // refs
    JsonNode result = execute("{Pet_groupBy{count,tags{name}}}");

    assertEquals("blue", result.at("/Pet_groupBy/0/tags/name").textValue());
    assertEquals(1, result.at("/Pet_groupBy/0/count").intValue());

    assertEquals("green", result.at("/Pet_groupBy/1/tags/name").asText());
    assertEquals(3, result.at("/Pet_groupBy/1/count").intValue());

    assertEquals("purple", result.at("/Pet_groupBy/2/tags/name").textValue());
    assertEquals(2, result.at("/Pet_groupBy/2/count").intValue());

    assertEquals("red", result.at("/Pet_groupBy/3/tags/name").textValue());
    assertEquals(4, result.at("/Pet_groupBy/3/count").intValue());

    assertEquals(null, result.at("/Pet_groupBy/4/tags/name").textValue());
    assertEquals(1, result.at("/Pet_groupBy/4/count").intValue());

    result = execute("{Pet_groupBy{count,category{name}}}");
    assertEquals(1, result.at("/Pet_groupBy/0/count").intValue());
    assertEquals("ant", result.at("/Pet_groupBy/0/category/name").textValue());
    assertEquals("bird", result.at("/Pet_groupBy/1/category/name").textValue());

    result = execute("{Pet_groupBy{count,tags{name},category{name}}}");

    assertEquals(1, result.at("/Pet_groupBy/0/count").intValue());
    assertEquals("mouse", result.at("/Pet_groupBy/0/category/name").textValue());
    assertEquals("blue", result.at("/Pet_groupBy/0/tags/name").textValue());

    assertEquals(1, result.at("/Pet_groupBy/1/count").intValue());
    assertEquals("ant", result.at("/Pet_groupBy/1/category/name").textValue());
    assertEquals("green", result.at("/Pet_groupBy/1/tags/name").textValue());
    // 1 green ant
    assertEquals(1, result.at("/Pet_groupBy/2/count").intValue());
    assertEquals("caterpillar", result.at("/Pet_groupBy/2/category/name").textValue());
    assertEquals("green", result.at("/Pet_groupBy/2/tags/name").textValue());

    // also works on refback
    result = execute("{Pet_groupBy {count,orders{orderId}}}");
    // 6 pets without order
    assertEquals(6, result.at("/Pet_groupBy/2/count").intValue());
    assertNull(null, result.at("/Pet_groupBy/2/orders").textValue());

    // orderId=1 has one pet
    assertEquals(1, result.at("/Pet_groupBy/1/count").intValue());
    assert (result.at("/Pet_groupBy/1/orders/orderId").textValue().contains("ORDER:"));

    // apply nested filter
    result =
        execute(
            "{User{username,pets_groupBy(filter:{name:{equals:\"the very hungry caterpillar\"}}){count,tags{name}}}}");
    assertEquals(1, result.at("/User/0/pets_groupBy/0/count").intValue());
    assertEquals("green", result.at("/User/0/pets_groupBy/0/tags/name").textValue());
  }

  @Test
  public void testGroupByWithSpaces() throws IOException {
    // rename column 'category' to 'category_test' and 'tag' to 'tag test' and 'name' to 'name test'
    Column newCategory = schema.getTable("Pet").getMetadata().getColumn("category");
    newCategory.setName("category test");
    Column newTags = schema.getTable("Pet").getMetadata().getColumn("tags");
    newTags.setName("tags test");
    Column newCategoryName = schema.getTable("Category").getMetadata().getColumn("name");
    newCategoryName.setName("name test");
    Column newTagName = schema.getTable("Tag").getMetadata().getColumn("name");
    newTagName.setName("name test");
    schema.getTable("Pet").getMetadata().alterColumn("category", newCategory);
    schema.getTable("Pet").getMetadata().alterColumn("tags", newTags);
    schema.getTable("Category").getMetadata().alterColumn("name", newCategoryName);
    schema.getTable("Tag").getMetadata().alterColumn("name", newTagName);

    // refs
    JsonNode result = execute("{Pet_groupBy{count,_sum{weight},tagsTest{nameTest}}}");

    assertEquals(null, result.at("/Pet_groupBy/4/tagsTest/nameTest").textValue());
    assertEquals(1, result.at("/Pet_groupBy/4/count").intValue());
    assertEquals(9.4d, result.at("/Pet_groupBy/4/_sum/weight").doubleValue());

    assertEquals("blue", result.at("/Pet_groupBy/0/tagsTest/nameTest").asText());
    assertEquals(1, result.at("/Pet_groupBy/0/count").intValue());

    assertEquals("green", result.at("/Pet_groupBy/1/tagsTest/nameTest").textValue());
    assertEquals(3, result.at("/Pet_groupBy/1/count").intValue());

    result = execute("{Pet_groupBy{count,categoryTest{nameTest}}}");
    assertEquals(1, result.at("/Pet_groupBy/0/count").intValue());
    assertEquals("ant", result.at("/Pet_groupBy/0/categoryTest/nameTest").textValue());
    assertEquals("bird", result.at("/Pet_groupBy/1/categoryTest/nameTest").textValue());

    // currently doensn't contain cat because somehow 'null' are not included
    result = execute("{Pet_groupBy{count,tagsTest{nameTest},categoryTest{nameTest}}}");
    // 1 <untagged> cat
    assertEquals(1, result.at("/Pet_groupBy/6/count").intValue());
    assertEquals("cat", result.at("/Pet_groupBy/6/categoryTest/nameTest").textValue());
    assertEquals(null, result.at("/Pet_groupBy/6/tagsTest/nameTest").textValue());
    // 1 blue mouse
    assertEquals(1, result.at("/Pet_groupBy/10/count").intValue());
    assertEquals("mouse", result.at("/Pet_groupBy/10/categoryTest/nameTest").textValue());
    assertEquals("blue", result.at("/Pet_groupBy/10/tagsTest/nameTest").textValue());
    // 1 green ant
    assertEquals(1, result.at("/Pet_groupBy/0/count").intValue());
    assertEquals("ant", result.at("/Pet_groupBy/0/categoryTest/nameTest").textValue());
    assertEquals("green", result.at("/Pet_groupBy/0/tagsTest/nameTest").textValue());

    // N.B. in case arrays are involved total might more than count!!!

    // undo rename column with spaces for any other test
    newCategory = schema.getTable("Pet").getMetadata().getColumn("category test");
    newCategory.setName("category");
    newTags = schema.getTable("Pet").getMetadata().getColumn("tags test");
    newTags.setName("tags");
    newCategoryName = schema.getTable("Category").getMetadata().getColumn("name test");
    newCategoryName.setName("name");
    newTagName = schema.getTable("Tag").getMetadata().getColumn("name test");
    newTagName.setName("name");
    schema.getTable("Pet").getMetadata().alterColumn("category test", newCategory);
    schema.getTable("Pet").getMetadata().alterColumn("tags test", newTags);
    schema.getTable("Category").getMetadata().alterColumn("name test", newCategoryName);
    schema.getTable("Tag").getMetadata().alterColumn("name test", newTagName);
  }

  @Test
  public void testSchemaQueries() throws IOException {
    assertEquals(schemaName, execute("{_schema{name}}").at("/_schema/name").textValue());
  }

  @Test
  public void testMembersOperations() throws IOException {

    // list members
    int count = execute("{_schema{members{email}}}").at("/_schema/members").size();

    // add members
    execute("mutation{change(members:{email:\"blaat\", role:\"Manager\"}){message}}");
    assertEquals(count + 1, execute("{_schema{members{email}}}").at("/_schema/members").size());

    // remove members
    execute("mutation{drop(members:\"blaat\"){message}}");
    assertEquals(count, execute("{_schema{members{email}}}").at("/_schema/members").size());
  }

  @Test
  public void testTableAlterDropOperations() throws IOException {
    // simple meta
    int tables = execute("{_schema{tables{name}}}").at("/_schema/tables").size();
    assertEquals(5, tables);

    // add table
    execute(
        "mutation{change(tables:[{name:\"table1\",labels:[{locale:\"en\", value: \"table1\"}],descriptions:[{locale:\"en\", value: \"desc1\"}],columns:[{name:\"col1\", defaultValue: \"bla\", key:1, labels:[{locale:\"en\", value:\"column1\"}], descriptions:[{locale:\"en\", value:\"desc11\"}]}]}]){message}}");
    JsonNode node =
        execute(
            "{_schema{tables{name,labels{locale,value},descriptions{locale,value},columns{name,key,defaultValue,labels{locale,value},descriptions{locale,value}}}}}");
    assertEquals(1, node.at("/_schema/tables/5/columns/0/key").intValue());
    assertEquals("bla", node.at("/_schema/tables/5/columns/0/defaultValue").asText());

    assertEquals("en", node.at("/_schema/tables/5/labels/0/locale").asText());
    assertEquals("table1", node.at("/_schema/tables/5/labels/0/value").asText());

    assertEquals("en", node.at("/_schema/tables/5/descriptions/0/locale").asText());
    assertEquals("desc1", node.at("/_schema/tables/5/descriptions/0/value").asText());

    assertEquals("en", node.at("/_schema/tables/5/columns/0/labels/0/locale").asText());
    assertEquals("column1", node.at("/_schema/tables/5/columns/0/labels/0/value").asText());

    assertEquals("en", node.at("/_schema/tables/5/columns/0/descriptions/0/locale").asText());
    assertEquals("desc11", node.at("/_schema/tables/5/columns/0/descriptions/0/value").asText());

    assertEquals(6, execute("{_schema{tables{name}}}").at("/_schema/tables").size());

    // drop
    execute("mutation{drop(tables:\"table1\"){message}}");
    assertEquals(5, execute("{_schema{tables{name}}}").at("/_schema/tables").size());
  }

  private JsonNode execute(String query) throws IOException {
    String result =
        convertExecutionResultToJson(session.getGraphqlForSchema(schemaName).execute(query));
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
    assertEquals(count + 1, execute("{Tag_agg{count}}").at("/Tag_agg/count").intValue());
    // delete
    execute("mutation{delete(Tag:{name:\"blaat\"}){message}}");
    assertEquals(count, execute("{Tag_agg{count}}").at("/Tag_agg/count").intValue());
  }

  @Test
  public void testAddAlterDropColumn() throws IOException {
    execute("mutation{change(columns:{table:\"Pet\",name:\"test\"}){message}}");
    assertNotNull(
        session
            .getDatabase()
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test"));
    execute(
        "mutation{change(columns:{table:\"Pet\", oldName:\"test\",name:\"test2\", key:3, columnType:\"INT\"}){message}}");

    assertNull(
        session
            .getDatabase()
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test"));
    assertEquals(
        ColumnType.INT,
        session
            .getDatabase()
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2")
            .getColumnType());

    execute("mutation{drop(columns:[{table:\"Pet\", column:\"test2\"}]){message}}");

    assertNull(
        session
            .getDatabase()
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2"));

    execute(
        "mutation{change(columns:{table:\"Pet\", name:\"test2\", columnType:\"STRING\", visible:\"blaat\"}){message}}");
    assertEquals(
        "blaat",
        session
            .getDatabase()
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2")
            .getVisible());
    execute("mutation{drop(columns:[{table:\"Pet\", column:\"test2\"}]){message}}");

    execute(
        "mutation{change(columns:{table:\"Pet\", name:\"test2\", columnType:\"STRING\", computed:\"blaat2\"}){message}}");
    assertEquals(
        "blaat2",
        session
            .getDatabase()
            .getSchema(schemaName)
            .getTable("Pet")
            .getMetadata()
            .getColumn("test2")
            .getComputed());
    execute("mutation{drop(columns:[{table:\"Pet\", column:\"test2\"}]){message}}");
  }

  @Test
  public void testNamesWithSpaces() throws IOException {
    try {
      schemaName = "testNamesWithSpaces";
      Schema myschema = session.getDatabase().dropCreateSchema(schemaName);

      // test escaping
      assertEquals("firstName", convertToCamelCase("First name"));
      assertEquals("firstName", convertToCamelCase("First  name"));
      assertEquals("first_name", convertToCamelCase("first_name"));

      assertEquals("FirstName", convertToPascalCase("first name"));
      assertEquals("FirstName", convertToPascalCase("first  name"));
      assertEquals("First_name", convertToPascalCase("first_name"));

      System.out.println(convertToCamelCase("Person details"));

      myschema.create(
          table(
              "Person details",
              column("First name").setPkey(),
              column("Last_name").setPkey(),
              column("some number").setType(ColumnType.INT)),
          table(
              "Some",
              column("id").setPkey(),
              column("Person details").setType(REF).setRefTable("Person details"),
              column("Persons details").setType(REF_ARRAY).setRefTable("Person details")),
          table(
              "Child details",
              column("Parent details").setPkey().setType(REF).setRefTable("Person details"),
              column("name").setPkey()),
          table(
              "Other",
              column("id").setPkey(),
              column("Child details").setType(REF).setRefTable("Child details")));

      execute(
          "mutation{insert(PersonDetails:{firstName:\"blaata\",last_name:\"blaata2\",someNumber: 6}){message}}");

      int count = execute("{PersonDetails_agg{count}}").at("/PersonDetails_agg/count").intValue();

      // insert should increase count
      execute(
          "mutation{insert(PersonDetails:{firstName:\"blaatb\",last_name:\"blaatb2\"}){message}}");
      assertEquals(
          count + 1,
          execute("{PersonDetails_agg{count}}").at("/PersonDetails_agg/count").intValue());

      // order by should work with spaces
      assertEquals(
          "blaata",
          execute("{PersonDetails(orderby:{firstName:ASC}){firstName}}")
              .at("/PersonDetails/0/firstName")
              .asText());

      assertEquals(
          "blaatb",
          execute("{PersonDetails(orderby:{firstName:DESC}){firstName}}")
              .at("/PersonDetails/0/firstName")
              .asText());

      // order by should work with underscore
      assertEquals(
          "blaata2",
          execute("{PersonDetails(orderby:{last_name:ASC}){last_name}}")
              .at("/PersonDetails/0/last_name")
              .asText());

      assertEquals(
          "blaatb2",
          execute("{PersonDetails(orderby:{last_name:DESC}){last_name}}")
              .at("/PersonDetails/0/last_name")
              .asText());

      // aggregates should be working with spaces too
      JsonNode agg =
          execute(
              "{PersonDetails_agg{_sum{someNumber}avg{someNumber}min{someNumber}max{someNumber}}}");
      assertEquals(6, agg.at("/PersonDetails_agg/_sum/someNumber").asInt());
      assertEquals(6, agg.at("/PersonDetails_agg/avg/someNumber").asInt());
      assertEquals(6, agg.at("/PersonDetails_agg/min/someNumber").asInt());
      assertEquals(6, agg.at("/PersonDetails_agg/max/someNumber").asInt());

      // equals key filters
      execute(
          "mutation{insert(Some:{id:\"one\",personDetails:{firstName:\"blaata\",last_name:\"blaata2\"},personsDetails:[{firstName:\"blaata\",last_name:\"blaata2\"}]}){message}}");
      JsonNode sub =
          execute(
              "{Some(filter:{personDetails:{equals:{firstName:\"blaata\",last_name:\"blaata2\"}}}){id}}");
      assertEquals("one", sub.at("/Some/0/id").asText());

      // equals nested key filters
      execute(
          "mutation{insert(ChildDetails:{name:\"b\",parentDetails:{firstName:\"blaata\",last_name:\"blaata2\"}}){message}}");
      execute(
          "mutation{insert(Other:{id:\"one\",childDetails:{name:\"b\",parentDetails:{firstName:\"blaata\",last_name:\"blaata2\"}}}){message}}");

      JsonNode subsub =
          execute(
              "{Other(filter:{childDetails:{equals:{name:\"b\",parentDetails:{firstName:\"blaata\",last_name:\"blaata2\"}}}}){id}}");
      assertEquals("one", subsub.at("/Other/0/id").asText());

      // delete
      execute("mutation{delete(Other:{id:\"one\"}){message}}");
      execute(
          "mutation{delete(ChildDetails:{name:\"b\",parentDetails:{firstName:\"blaata\",last_name:\"blaata2\"}}){message}}");
      execute("mutation{delete(Some:{id:\"one\"}){message}}");
      execute(
          "mutation{delete(PersonDetails:{firstName:\"blaata\",last_name:\"blaata2\"}){message}}");
      execute(
          "mutation{delete(PersonDetails:{firstName:\"blaatb\",last_name:\"blaata2\"}){message}}");
      assertEquals(
          count, execute("{PersonDetails_agg{count}}").at("/PersonDetails_agg/count").intValue());

      // truncate should also work with identifier
      execute("mutation{truncate(tables: \"PersonDetails\"){message}}");
      assertEquals(
          0, execute("{PersonDetails_agg{count}}").at("/PersonDetails_agg/count").intValue());

      // reset to default schema name
    } finally {
      schemaName = TestGraphqlSchemaFields.class.getSimpleName();
    }
  }

  @Test
  public void testTableType() throws IOException {
    JsonNode result = execute("{_schema{name,tables{name,tableType}}}");
    assertEquals("DATA", result.at("/_schema/tables/0/tableType").asText(), "DATA");
    assertEquals("ONTOLOGIES", result.at("/_schema/tables/3/tableType").asText());
  }

  @Test
  public void testJsonType() throws IOException {
    try {
      schemaName = "testJsonType";

      Schema myschema = session.getDatabase().dropCreateSchema(schemaName);
      myschema.create(
          table("TestJson", column("name").setPkey(), column("json").setType(ColumnType.JSON)));

      Table table = myschema.getTable("TestJson");
      String value = "{\"name\":\"bofke\"}";
      table.insert(row("name", "test", "json", value));

      assertEquals(value, execute("{TestJson{json}}").at("/TestJson/0/json").asText());

      String value2 = "{\"name\":\"bofke2\"}";
      Map data = new LinkedHashMap();
      data.put("name", "test");
      data.put("json", value2);
      session
          .getGraphqlForSchema(schemaName)
          .execute(
              new ExecutionInput.Builder()
                  .query(
                      "mutation update($value:[TestJsonInput]){update(TestJson:$value){message}}")
                  .variables(Map.of("value", data))
                  .build());

      assertEquals(value2, execute("{TestJson{json}}").at("/TestJson/0/json").asText());
      assertEquals(
          value2,
          execute(
                  "{TestJson(filter:{json:{equals:\"{\\\"name\\\": \\\"bofke2\\\"}\"}}){json}}") // notice the extra space!
              .at("/TestJson/0/json")
              .asText());
      // disabled because like doesn't make sense like this. we should revisit json queries.
      //      assertEquals(
      //          value2,
      //          execute("{TestJson(filter:{json:{like:\"bofke2\"}}){json}}") // more useful but
      // inconsistent
      //              .at("/TestJson/0/json")
      //              .asText());
    } finally {
      // reset to default schema name
      schemaName = TestGraphqlSchemaFields.class.getSimpleName();
    }
  }

  @Test
  public void testFileType() throws IOException {
    try {
      schemaName = "testFileType";
      Schema myschema = session.getDatabase().dropCreateSchema(schemaName);
      myschema.create(
          table("TestFile", column("name").setPkey(), column("image").setType(ColumnType.FILE)));

      // insert file (note: ideally here also use mutation but I don't know how to add file part to
      // request)
      Table table = myschema.getTable("TestFile");
      table.insert(
          row(
              "name",
              "test",
              "image",
              new BinaryFileWrapper("text/html", "testfile.txt", "test".getBytes())));

      assertEquals(4, execute("{TestFile{image{size}}}").at("/TestFile/0/image/size").asInt());

      // update with {} existing file metadata should keep file untouched
      Map data = new LinkedHashMap();
      data.put("name", "test");
      data.put("image", Map.of("name", "dummy"));
      session
          .getGraphqlForSchema(schemaName)
          .execute(
              new ExecutionInput.Builder()
                  .query(
                      "mutation update($value:[TestFileInput]){update(TestFile:$value){message}}")
                  .variables(Map.of("value", data))
                  .build());
      assertEquals(4, execute("{TestFile{image{size}}}").at("/TestFile/0/image/size").asInt());

      // update with null should delete
      data.put("image", null);
      session
          .getGraphqlForSchema(schemaName)
          .execute(
              new ExecutionInput.Builder()
                  .query(
                      "mutation update($value:[TestFileInput]){update(TestFile:$value){message}}")
                  .variables(Map.of("value", data))
                  .build());
      assertEquals(
          0,
          execute("{TestFile{image{size,filename,extension,url}}}")
              .at("/TestFile/0/image/size")
              .asInt());
    } finally {
      // reset to default schema name
      schemaName = TestGraphqlSchemaFields.class.getSimpleName();
    }
  }

  @Test
  public void testTasksApi() throws IOException {
    // fake something into taskservice
    Task task = new Task("test");
    task.addSubTask(new Task("subtest"));
    session.getTaskService().submit(task);

    // list all tasks
    assertTrue(
        execute("{_tasks{id,description,status}}")
            .at("/_tasks/0/description")
            .textValue()
            .startsWith("test"));
    // load single task
    assertTrue(
        execute(
                "{_tasks(id:\""
                    + task.getId()
                    + "\"){id,description,status,subTasks{id,description,status,subTasks{id,description,status}}}}")
            .at("/_tasks/0/description")
            .textValue()
            .startsWith("test"));
  }

  @Test
  public void testTruncate() throws IOException {
    List<Row> result = schema.getTable("Order").retrieveRows();
    String message =
        execute("mutation {truncate(tables: \"Order\"){message}}").at("/truncate/message").asText();
    assertTrue(message.contains("Truncated"));
    List<Row> result2 = schema.getTable("Order").retrieveRows();
    assertTrue(result.size() > 0 && result2.size() == 0);

    // restore
    schema = session.getDatabase().dropCreateSchema(schemaName);
    PET_STORE.getImportTask(schema, true).run();
  }

  @Test
  public void testTruncateAsync() throws IOException, InterruptedException {
    List<Row> preTruncatedResult = schema.getTable("Order").retrieveRows();
    String taskId =
        execute("mutation {truncate(tables: \"Order\" async:true){ taskId message}}")
            .at("/truncate/taskId")
            .asText();

    String status = "";
    int pollCount = 0;
    while (!"COMPLETED".equals(status) && !"ERROR".equals(status)) {
      status =
          execute("{ _tasks( id: \"" + taskId + "\"){ status }}")
              .get("_tasks")
              .get(0)
              .get("status")
              .asText();
      if (pollCount++ > 5) {
        throw new MolgenisException("failed: polling took too long, result is: " + status);
      }
      Thread.sleep(1000);
    }

    List<Row> truncatedResult = schema.getTable("Order").retrieveRows();
    assertTrue(!preTruncatedResult.isEmpty() && truncatedResult.isEmpty());

    // restore
    schema = session.getDatabase().dropCreateSchema(schemaName);
    PET_STORE.getImportTask(schema, true).run();
  }

  @Test
  public void testReport() throws IOException {
    schema = session.getDatabase().dropCreateSchema(schemaName);
    PET_STORE.getImportTask(schema, true).run();

    JsonNode result = execute("{_reports(id:\"report1\"){data,count}}");
    assertTrue(result.at("/_reports/data").textValue().contains("pooky"));
    assertEquals(8, result.at("/_reports/count").intValue());

    // report 2 has parameters
    result =
        execute(
            "{_reports(id:\"report2\",parameters:{key:\"name\", value:\"spike\"}){data,count}}");
    assertTrue(result.at("/_reports/data").textValue().contains("spike"));
    assertEquals(1, result.at("/_reports/count").intValue());

    // report by id=report1
    result =
        execute(
            "{_reports(id:\"report2\",parameters:{key:\"name\", value:\"spike\"}){data,count}}");
    assertTrue(result.at("/_reports/data").textValue().contains("spike"));
    assertEquals(1, result.at("/_reports/count").intValue());
  }
}
