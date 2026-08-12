package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class GraphqlExecutorTest {

  private static final String SCHEMA_NAME = GraphqlExecutorTest.class.getSimpleName();
  private static GraphqlExecutor graphql;
  private static GraphQL rawGraphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    database.createSchema(SCHEMA_NAME);
    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());
    rawGraphql = GraphqlFactory.forDatabase(database, new TaskServiceInMemory());
  }

  @Test
  void executeWithMultipleErrors_throwsFirstMessageAndSuppressesTheRest() {
    GraphqlExecutor.DummySessionHandler sessionHandler = new GraphqlExecutor.DummySessionHandler();
    String queryWithTwoFailingFields =
        "{a: _tasks(id:\""
            + SCHEMA_NAME
            + "NoSuchTaskA\"){id} b: _tasks(id:\""
            + SCHEMA_NAME
            + "NoSuchTaskB\"){id}}";

    ExecutionResult rawResult =
        rawGraphql.execute(ExecutionInput.newExecutionInput(queryWithTwoFailingFields).build());
    List<GraphQLError> rawErrors = rawResult.getErrors();
    assertEquals(2, rawErrors.size(), "expected the query to produce two errors: " + rawErrors);

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> graphql.execute(queryWithTwoFailingFields, null, sessionHandler));

    assertEquals(rawErrors.get(0).getMessage(), exception.getMessage());
    assertEquals(1, exception.getSuppressed().length);
    assertEquals(rawErrors.get(1).getMessage(), exception.getSuppressed()[0].getMessage());
  }
}
