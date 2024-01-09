package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Privileges.AGGREGATOR;
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
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class TestGraphqlAggregatePermission {
  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = TestGraphqlAggregatePermission.class.getSimpleName();
  private static Schema schema;
  private static TaskService taskService;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    new PetStoreLoader().load(schema, true);
    schema.removeMember(ANONYMOUS);
    schema.addMember("AGGREGATE_TEST_USER", AGGREGATOR.toString());
    database.setActiveUser("AGGREGATE_TEST_USER");
    taskService = new TaskServiceInMemory();
    grapql =
        new GraphqlApiFactory().createGraphqlForSchema(database.getSchema(schemaName), taskService);
  }

  @Test
  public void aggregateShouldNotHaveTableGraphql() {
    assertThrows(MolgenisException.class, () -> execute("{Pet{name}}"));
  }

  @Test
  public void aggregateShouldHaveOntologyGraphql() {
    assertDoesNotThrow(() -> execute("{Tag{name}}"));
  }

  @Test
  public void aggregateShouldHaveCountGraphql() {
    assertDoesNotThrow(() -> execute("{Pet_agg{count}}"));
  }

  @Test
  public void aggregateShouldHaveGroupByGraphql() {
    assertDoesNotThrow(() -> execute("{Pet_groupBy{count,tags{name}}}"));
  }

  @Test
  public void aggregateShouldNotHaveGroupByOtherTable() {
    assertThrows(MolgenisException.class, () -> execute("{Pet_groupBy{count,category{name}}"));
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
