package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.molgenis.emx2.datamodels.test.CrossSchemaReferenceExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlCrossSchemaRefs {

  private static final String schemaName1 = TestGraphqlCrossSchemaRefs.class.getSimpleName() + "1";
  private static final String schemaName2 = TestGraphqlCrossSchemaRefs.class.getSimpleName() + "2";

  private static GraphQL graphql;
  private static Schema schema1;
  private static Schema schema2;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(schemaName2);
    database.dropSchemaIfExists(schemaName1);
    schema1 = database.createSchema(schemaName1);
    schema2 = database.createSchema(schemaName2);

    CrossSchemaReferenceExample.create(schema1, schema2);
    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema2);
  }

  @Test
  public void test() throws IOException {
    assertEquals(
        "parent1", execute("{Child{name,parent{name}}}").at("/Child/0/parent/name").asText());

    assertEquals(
        "dog", execute("{PetLover{name,pets{species}}}").at("/PetLover/0/pets/1/species").asText());

    // checks for a reported bug that ChildInput were not created
    assertTrue(
        execute("mutation{save(Child:{name:\"test\"}){message}}")
            .at("/save/message")
            .asText()
            .contains("upserted"));
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
