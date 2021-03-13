package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.CrossSchemaReferenceExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlCrossSchemaRefs {

  private static final String schemaName1 = TestGraphqlCrossSchemaRefs.class.getSimpleName() + "1";
  private static final String schemaName2 = TestGraphqlCrossSchemaRefs.class.getSimpleName() + "2";

  private static GraphQL graphql;
  private static Schema schema1;
  private static Schema schema2;

  @BeforeClass
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema1 = database.dropCreateSchema(schemaName1);
    schema2 = database.dropCreateSchema(schemaName2);

    CrossSchemaReferenceExample.create(schema1, schema2);
    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema2);
  }

  @Test
  public void test() throws IOException {
    Assert.assertEquals(
        "parent1", execute("{Child{name,parent{name}}}").at("/Child/0/parent/name").asText());

    Assert.assertEquals(
        "dog", execute("{PetLover{name,pets{species}}}").at("/PetLover/0/pets/1/species").asText());

    Assert.assertTrue(
        execute("{_schema{tables{name}}}")
            .at("/_schema/tables")
            .findValuesAsText("name")
            .contains("Parent"));
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText(), "");
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
