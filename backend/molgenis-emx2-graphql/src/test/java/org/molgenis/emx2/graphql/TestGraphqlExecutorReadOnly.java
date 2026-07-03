package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class TestGraphqlExecutorReadOnly {

  private static final String SCHEMA_NAME = TestGraphqlExecutorReadOnly.class.getSimpleName();
  private static Database database;
  private static GraphqlExecutor graphql;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());
  }

  @Test
  public void readOnlyAllowsQueries() {
    assertNotNull(
        graphql
            .executeReadOnly("{_schemas{name}}", null, new GraphqlExecutor.DummySessionHandler())
            .getData());
    // also named query operations and introspection
    assertNotNull(
        graphql
            .executeReadOnly(
                "query listSchemas {_schemas{name}} ",
                null,
                new GraphqlExecutor.DummySessionHandler())
            .getData());
    assertNotNull(
        graphql
            .executeReadOnly(
                "{__schema{types{name}}}", null, new GraphqlExecutor.DummySessionHandler())
            .getData());
  }

  @Test
  public void readOnlyRejectsMutations() {
    String mutation = "mutation{createSchema(name:\"" + SCHEMA_NAME + "\"){message}}";
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                graphql.executeReadOnly(mutation, null, new GraphqlExecutor.DummySessionHandler()));
    assertTrue(exception.getMessage().contains("Only query operations are allowed"));
    assertNull(database.getSchema(SCHEMA_NAME));
  }

  @Test
  public void readOnlyRejectsNamedMutations() {
    String mutation = "mutation harmless {createSchema(name:\"" + SCHEMA_NAME + "\"){message}}";
    assertThrows(
        MolgenisException.class,
        () -> graphql.executeReadOnly(mutation, null, new GraphqlExecutor.DummySessionHandler()));
    assertNull(database.getSchema(SCHEMA_NAME));
  }

  @Test
  public void readOnlyRejectsMutationHiddenBehindQuery() {
    // a document can contain multiple operations; none of them may be a mutation
    String document =
        "query q {_schemas{name}} mutation m {createSchema(name:\"" + SCHEMA_NAME + "\"){message}}";
    assertThrows(
        MolgenisException.class,
        () -> graphql.executeReadOnly(document, null, new GraphqlExecutor.DummySessionHandler()));
    assertNull(database.getSchema(SCHEMA_NAME));
  }

  @Test
  public void readOnlyRejectsMutationsWithVariables() {
    String mutation = "mutation($name:String){createSchema(name:$name){message}}";
    assertThrows(
        MolgenisException.class,
        () ->
            graphql.executeReadOnly(
                mutation, Map.of("name", SCHEMA_NAME), new GraphqlExecutor.DummySessionHandler()));
    assertNull(database.getSchema(SCHEMA_NAME));
  }

  @Test
  public void executeStillAllowsMutations() {
    try {
      database.dropSchemaIfExists(SCHEMA_NAME);
      graphql.execute(
          "mutation{createSchema(name:\"" + SCHEMA_NAME + "\"){message}}",
          null,
          new GraphqlExecutor.DummySessionHandler());
      assertNotNull(database.getSchema(SCHEMA_NAME));
    } finally {
      database.dropSchemaIfExists(SCHEMA_NAME);
    }
  }
}
