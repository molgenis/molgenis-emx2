package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

@Tag("slow")
public class TestAutoIdGeneration {
  static Database db;
  static Schema schema;

  private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int ID_LENGTH = 8;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestAutoIdGeneration.class.getSimpleName());
  }

  @Test
  public void testAutoIdWithCharsetAndLength() {
    Table t =
        schema.create(
            table(
                "TestAutoId",
                column("id")
                    .setType(ColumnType.AUTO_ID)
                    .setComputed("${mg_autoid(charset=" + CHARSET + ", length=" + ID_LENGTH + ")}")
                    .setPkey(),
                column("name")));

    // Insert multiple rows
    for (int i = 0; i < 10; i++) {
      t.insert(new Row().set("name", "item" + i));
    }

    List<Row> rows = t.query().retrieveRows();
    assertEquals(10, rows.size());

    Set<String> ids = new HashSet<>();
    for (Row row : rows) {
      String id = row.getString("id");
      assertNotNull(id);
      assertEquals(ID_LENGTH, id.length(), "ID should have length " + ID_LENGTH);
      // Verify all characters are from the charset
      for (char c : id.toCharArray()) {
        assertTrue(CHARSET.indexOf(c) >= 0, "Character '" + c + "' not in charset");
      }
      ids.add(id);
    }
    // All IDs should be unique
    assertEquals(10, ids.size(), "All IDs should be unique");
  }

  @Test
  public void testAutoIdWithPrefix() {
    Table t =
        schema.create(
            table(
                "TestAutoIdPrefix",
                column("id")
                    .setType(ColumnType.AUTO_ID)
                    .setComputed(
                        "PRJ-${mg_autoid(charset=" + CHARSET + ", length=" + ID_LENGTH + ")}")
                    .setPkey(),
                column("description")));

    t.insert(new Row().set("description", "test"));

    List<Row> rows = t.query().retrieveRows();
    assertEquals(1, rows.size());
    String id = rows.get(0).getString("id");
    assertTrue(id.startsWith("PRJ-"), "ID should start with prefix 'PRJ-'");
    assertEquals("PRJ-".length() + ID_LENGTH, id.length());
  }

  @Test
  public void testAutoIdParallelInserts() throws Exception {
    Table t =
        schema.create(
            table(
                "TestAutoIdParallel",
                column("id")
                    .setType(ColumnType.AUTO_ID)
                    .setComputed("${mg_autoid(charset=" + CHARSET + ", length=" + ID_LENGTH + ")}")
                    .setPkey(),
                column("value")));

    int numThreads = 5;
    int insertsPerThread = 10;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<?>> futures = new ArrayList<>();

    for (int thread = 0; thread < numThreads; thread++) {
      final int threadNum = thread;
      futures.add(
          executor.submit(
              () -> {
                for (int i = 0; i < insertsPerThread; i++) {
                  // Each thread uses its own database session
                  Database threadDb = TestDatabaseFactory.getTestDatabase();
                  Schema threadSchema =
                      threadDb.getSchema(TestAutoIdGeneration.class.getSimpleName());
                  Table threadTable = threadSchema.getTable("TestAutoIdParallel");
                  threadTable.insert(new Row().set("value", "thread" + threadNum + "_item" + i));
                }
              }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

    // Check all futures completed without exception
    for (Future<?> future : futures) {
      assertDoesNotThrow(() -> future.get());
    }

    List<Row> rows = t.query().retrieveRows();
    int expectedTotal = numThreads * insertsPerThread;
    assertEquals(expectedTotal, rows.size());

    // Verify all IDs are unique
    Set<String> ids = new HashSet<>();
    for (Row row : rows) {
      ids.add(row.getString("id"));
    }
    assertEquals(expectedTotal, ids.size(), "All IDs should be unique across parallel inserts");
  }
}
