package org.molgenis.emx2.web;

import graphql.ExecutionResult;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestGraphqlApiForDatabase {

  private static Database database;
  private static final String schemaName = "TestGraphqlApiForDatabase";

  @BeforeClass
  public static void setup() {
    database = DatabaseFactory.getTestDatabase();
    Schema schema = database.createSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
  }

  @Test
  public void test() {

    GraphqlApiForDatabase api = new GraphqlApiForDatabase(database);

    // list
    Map<String, Object> result = api.execute("{Schemas{name}}").toSpecification();
    int length = length(result);

    // create
    api.execute("mutation{createSchema(name:\"" + schemaName + "B\"){detail}}");

    // check listing again
    result = api.execute("{Schemas{name}}").toSpecification();
    assertEquals(length + 1, length(result));

    // remove
    api.execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){detail}}");

    // check listing again
    result = api.execute("{Schemas{name}}").toSpecification();
    assertEquals(length, length(result));
  }

  private int length(Map<String, Object> result) {
    Map map = (Map) result.get("data");
    List list = (List) map.get("Schemas");
    return list.size();
  }

  private String name(Map<String, Object> result, int i) {
    Map map = (Map) result.get("data");
    List list = (List) map.get("Schemas");
    Map<String, String> row = (Map) list.get(i);
    return row.get("name");
  }
}
