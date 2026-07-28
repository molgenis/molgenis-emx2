package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class GraphqlExecutorTest {

  private static final String SCHEMA_NAME = GraphqlExecutorTest.class.getSimpleName();
  private static Database database;
  private static GraphqlExecutor graphql;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    database.createSchema(SCHEMA_NAME);
    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());
  }

  @Test
  public void logsCause() {
    CapturingAppender appender = new CapturingAppender();
    org.apache.logging.log4j.core.Logger executorLogger =
        (org.apache.logging.log4j.core.Logger) LogManager.getLogger(GraphqlExecutor.class);
    appender.start();
    executorLogger.addAppender(appender);
    try {
      MolgenisException exception =
          assertThrows(
              MolgenisException.class,
              () ->
                  graphql.execute(
                      "mutation{createSchema(name:\"" + SCHEMA_NAME + "\"){message}}",
                      null,
                      new GraphqlExecutor.DummySessionHandler()));

      assertNotNull(exception.getCause());

      List<LogEvent> loggedThrowables =
          appender.getEvents().stream().filter(event -> event.getThrown() != null).toList();
      assertTrue(
          !loggedThrowables.isEmpty(),
          "expected the failing request to log a throwable, logged: " + appender.getEvents());
      Throwable logged = loggedThrowables.get(0).getThrown();
      assertTrue(logged.getStackTrace().length > 0);
    } finally {
      executorLogger.removeAppender(appender);
      appender.stop();
    }
  }

  @Test
  public void logsCauseOfExceptionThatIsNotMolgenisException() {
    CapturingAppender appender = new CapturingAppender();
    org.apache.logging.log4j.core.Logger executorLogger =
        (org.apache.logging.log4j.core.Logger) LogManager.getLogger(GraphqlExecutor.class);
    appender.start();
    executorLogger.addAppender(appender);
    try {
      MolgenisException exception =
          assertThrows(
              MolgenisException.class,
              () ->
                  graphql.execute(
                      "{_tasks(id:\"" + SCHEMA_NAME + "NoSuchTask\"){id}}",
                      null,
                      new GraphqlExecutor.DummySessionHandler()));

      assertInstanceOf(NullPointerException.class, exception.getCause());

      List<LogEvent> loggedThrowables =
          appender.getEvents().stream().filter(event -> event.getThrown() != null).toList();
      assertTrue(
          !loggedThrowables.isEmpty(),
          "expected the failing request to log a throwable, logged: " + appender.getEvents());
      assertInstanceOf(NullPointerException.class, loggedThrowables.get(0).getThrown());
    } finally {
      executorLogger.removeAppender(appender);
      appender.stop();
    }
  }

  private static class CapturingAppender extends AbstractAppender {
    private final List<LogEvent> events = Collections.synchronizedList(new ArrayList<>());

    CapturingAppender() {
      super("GraphqlExecutorTestCapture", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      events.add(event.toImmutable());
    }

    List<LogEvent> getEvents() {
      return events;
    }
  }
}
