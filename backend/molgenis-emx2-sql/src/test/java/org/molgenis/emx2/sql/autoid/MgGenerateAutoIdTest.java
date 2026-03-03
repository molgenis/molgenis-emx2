package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Row.row;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.generator.AutoIdFormat;

class MgGenerateAutoIdTest {

  private static final String SCHEMA_NAME = MgGenerateAutoIdTest.class.getSimpleName();
  private static final AutoIdFormat FORMAT = new AutoIdFormat(AutoIdFormat.Format.NUMBERS, 10);

  private DSLContext jooq;
  private Column column;
  private Table table;

  @BeforeEach
  void setup() {
    SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    jooq = database.getJooq();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    table = schema.create(TableMetadata.table("Person"));
    table.getMetadata().add(Column.column("id").setType(ColumnType.AUTO_ID).setPkey());
    column = table.getMetadata().getColumn("id");
  }

  @Test
  void givenEmptyCharset_thenThrowException() {
    assertThrows(
        DataAccessException.class,
        () -> generateAutoId(jooq, column, "", 10),
        "charset must not be empty");
  }

  @Test
  void givenZeroLength_thenThrowException() {
    assertThrows(
        DataAccessException.class,
        () -> generateAutoId(jooq, column, "abcd", 0),
        "id_length must be positive");
  }

  @Test
  void givenNegativeLength_thenThrowException() {
    assertThrows(
        DataAccessException.class,
        () -> generateAutoId(jooq, column, "abcd", -1),
        "id_length must be positive");
  }

  @Test
  void givenValidFormat_thenGenerateId() {
    resetSeed();
    String id = generateAutoId(jooq, column, FORMAT);
    assertEquals("9811681157", id);
  }

  @Test
  void givenGeneratedId_whenAlreadyExists_thenRetry() {
    resetSeed();
    table.insert(givenFirstNResultsForSeed(4).stream().map(id -> row("id", id)).toList());
    resetSeed();
    String id = generateAutoId(jooq, column, FORMAT);
    assertEquals("2187035124", id);
  }

  @Test
  void whenSurpassingMaxNrAttempts_thenThrowException() {
    resetSeed();
    table.insert(givenFirstNResultsForSeed(100).stream().map(id -> row("id", id)).toList());
    resetSeed();
    assertThrows(DataAccessException.class, () -> generateAutoId(jooq, column, FORMAT));
  }

  /**
   * Tests the behavior of concurrent transactions attempting to acquire an advisory lock
   * on the same column, ensuring that the lock is properly held and respected.
   *
   * <p>This test uses two threads:
   * <ul>
   *   <li><b>Thread 1:</b> Acquires an advisory lock by generating an auto ID for a column.
   *       It signals the second thread to start checking the lock status and retains the lock
   *       until the check is complete.</li>
   *   <li><b>Thread 2:</b> Attempts to acquire the same advisory lock after being signaled by Thread 1.
   *       It verifies that the lock is already held (i.e., the attempt fails), ensuring proper
   *       lock contention behavior.</li>
   * </ul>
   *
   * <p>The test uses {@link CountDownLatch} for thread coordination and {@link ExecutorService}
   * for concurrent execution. The advisory lock is implemented using PostgreSQL's
   * {@code pg_try_advisory_xact_lock} function, which is called via jOOQ.
   */
  @Test
  void givenConcurrentTransactions_whenRequestingSameColumn_thenLock()
      throws ExecutionException, InterruptedException {
    CountDownLatch lockLatch = new CountDownLatch(1);
    CountDownLatch checkedLatch = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<?> takeLockFuture =
        executor.submit(
            () -> {
              try (Connection conn = jooq.configuration().connectionProvider().acquire()) {
                DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
                ctx.transaction(
                    configuration -> {
                      // Generating this auto id retains a lock on transaction level
                      generateAutoId(ctx, column, FORMAT);
                      // Give the signal to the lock check transaction to start
                      checkedLatch.countDown();
                      // Retain the lock until the lock check in the other transaction is finished
                      lockLatch.await();
                    });
              } catch (Exception e) {
                fail("Failed to execute function", e);
              }
            });

    Future<?> checkLock =
        executor.submit(
            () -> {
              try (Connection conn = jooq.configuration().connectionProvider().acquire()) {
                DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
                ctx.transaction(
                    configuration -> {
                      checkedLatch.await();
                      Boolean lockAcquired =
                          ctx.select(
                                  DSL.field(
                                      "pg_try_advisory_xact_lock(hashtext('MgGenerateAutoIdTest.Person.id'))",
                                      Boolean.class))
                              .fetchOneInto(Boolean.class);
                      assertFalse(lockAcquired, "Advisory lock should be held by the function");
                      lockLatch.countDown();
                    });

              } catch (Exception e) {
                fail("Failed to check lock", e);
              }
            });

    takeLockFuture.get();
    checkLock.get();
  }

  private List<String> givenFirstNResultsForSeed(int n) {
    resetSeed();
    return IntStream.range(0, n)
        .boxed()
        .map(ignored -> generateAutoId(jooq, column, FORMAT))
        .toList();
  }

  private void resetSeed() {
    jooq.execute("SELECT setseed(0.5);");
  }

  private String generateAutoId(DSLContext jooq, Column column, AutoIdFormat format) {
    return generateAutoId(jooq, column, format.format().getCharacters(), format.length());
  }

  private String generateAutoId(DSLContext jooq, Column column, String charset, int length) {
    Record1<String> result =
        jooq.select(
                DSL.field(
                    "\"MOLGENIS\".mg_generate_autoid({0}, {1}, {2}, {3}, {4})",
                    String.class,
                    DSL.val(column.getSchemaName()),
                    DSL.val(column.getTableName()),
                    DSL.val(column.getName()),
                    DSL.val(charset),
                    DSL.val(length)))
            .fetchOne();

    assertNotNull(result);
    return result.value1();
  }
}
