package org.molgenis.emx2.graphql;

import static org.junit.Assert.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.graphql.GraphqlTableFieldFactory.escape;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class TestGraphqSchemaFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqlSchemaFields";
  private static TaskService taskService;
  private static Schema schema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    new PetStoreLoader().load(schema, true);
    taskService = new TaskServiceInMemory();
    grapql = new GraphqlApiFactory().createGraphqlForSchema(schema, taskService);
  }

  @Test
  public void testSession() throws IOException {
    try {
      database.setActiveUser(ANONYMOUS);
      TestCase.assertEquals(1, execute("{_session{email,roles}}").at("/_session/roles").size());
      execute("mutation { signin(email: \"shopmanager\",password:\"shopmanager\") {message}}");
      grapql =
          new GraphqlApiFactory()
              .createGraphqlForSchema(database.getSchema(schemaName), taskService);
      TestCase.assertTrue(execute("{_session{email,roles}}").toString().contains("Manager"));
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  public void testSchemaSettings() throws IOException {
    // add value
    execute("mutation{change(settings:{key:\"test\",value:\"testval\"}){message}}");

    assertEquals("testval", execute("{_settings{key,value}}").at("/_settings/0/value").textValue());

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
    assertEquals(3, execute("{_settings{key,value}}").at("/_settings").size());

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
    execute("mutation{drop(settings:[{table:\"Pet\", key:\"test\"}]){message}}");

    assertEquals(0, schema.getTable("Pet").getMetadata().getSettings().size());
    assertEquals(
        0,
        execute("{_schema{tables{settings{key,value}}}}").at("/_schema/tables/2/settings").size());
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
        execute("{Order_agg{max{quantity},min{quantity},sum{quantity},avg{quantity}}}")
            .at("/Order_agg/max/quantity")
            .intValue());

    // nested agg
    TestCase.assertEquals(
        15.7d,
        execute("{User{pets_agg{count,max{weight},min{weight},sum{weight},avg{weight}}}}")
            .at("/User/0/pets_agg/max/weight")
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
        "mutation{change(columns:{table:\"Pet\", name:\"test2\", columnType:\"STRING\", visible:\"blaat\"}){message}}");
    database.clearCache(); // cannot know here, server clears caches
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
  public void testNamesWithSpaces() throws IOException {
    try {
      Schema myschema = database.dropCreateSchema("testNamesWithSpaces");

      // test escaping
      assertEquals("first_name", escape("first name"));
      assertEquals("first_name", escape("first  name"));
      assertEquals("first__name", escape("first_name"));

      System.out.println(escape("Person details"));

      myschema.create(
          table("Person details", column("First name").setPkey(), column("Last name").setPkey()),
          table(
              "Some",
              column("id").setPkey(),
              column("person").setType(REF).setRefTable("Person details"),
              column("persons").setType(REF_ARRAY).setRefTable("Person details")));

      grapql = new GraphqlApiFactory().createGraphqlForSchema(myschema, taskService);

      int count = execute("{Person_details_agg{count}}").at("/Person_details_agg/count").intValue();

      // insert should increase count
      execute(
          "mutation{insert(Person_details:{First_name:\"blaat\",Last_name:\"blaat2\"}){message}}");
      TestCase.assertEquals(
          count + 1,
          execute("{Person_details_agg{count}}").at("/Person_details_agg/count").intValue());
      // delete
      execute(
          "mutation{delete(Person_details:{First_name:\"blaat\",Last_name:\"blaat2\"}){message}}");
      TestCase.assertEquals(
          count, execute("{Person_details_agg{count}}").at("/Person_details_agg/count").intValue());

      // reset
    } finally {
      grapql = new GraphqlApiFactory().createGraphqlForSchema(schema, taskService);
    }
  }

  @Test
  public void testTableType() throws IOException {
    JsonNode result = execute("{_schema{name,tables{name,tableType}}}");
    assertEquals("DATA", result.at("/_schema/tables/0/tableType").asText(), "DATA");
    assertEquals("ONTOLOGIES", result.at("/_schema/tables/3/tableType").asText());
  }

  @Test
  public void testFileType() throws IOException {
    try {
      Schema myschema = database.dropCreateSchema("testFileType");
      myschema.create(
          table("TestFile", column("name").setPkey(), column("image").setType(ColumnType.FILE)));

      grapql = new GraphqlApiFactory().createGraphqlForSchema(myschema, taskService);

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
      grapql.execute(
          new ExecutionInput.Builder()
              .query("mutation update($value:[TestFileInput]){update(TestFile:$value){message}}")
              .variables(Map.of("value", data))
              .build());
      assertEquals(4, execute("{TestFile{image{size}}}").at("/TestFile/0/image/size").asInt());

      // update with null should delete
      data.put("image", null);
      grapql.execute(
          new ExecutionInput.Builder()
              .query("mutation update($value:[TestFileInput]){update(TestFile:$value){message}}")
              .variables(Map.of("value", data))
              .build());
      assertEquals(
          0, execute("{TestFile{image{size,extension,url}}}").at("/TestFile/0/image/size").asInt());

      // reset
    } finally {
      grapql = new GraphqlApiFactory().createGraphqlForSchema(schema, taskService);
    }
  }

  @Test
  public void testTasksApi() throws IOException {
    // fake something into taskservice
    Task task = new Task("test");
    task.addSubTask(new Task("subtest"));
    taskService.submit(task);

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
    schema = database.dropCreateSchema(schemaName);
    new PetStoreLoader().load(schema, true);
  }
}
