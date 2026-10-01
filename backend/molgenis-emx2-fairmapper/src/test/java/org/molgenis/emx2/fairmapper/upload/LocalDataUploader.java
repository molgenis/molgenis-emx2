package org.molgenis.emx2.fairmapper.upload;

import graphql.AssertException;
import java.time.Duration;
import java.time.Instant;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.TableStore;

public class LocalDataUploader implements DataUploader {

  private final Schema schema;
  private final String[] tables;

  public LocalDataUploader(Schema schema, String... tables) {
    this.schema = schema;
    this.tables = tables;
  }

  @Override
  public void upload(TableStore tableStore) {
    ImportSchemaTask task =
        new ImportSchemaTask(tableStore, schema, false, tables)
            .setFilter(ImportSchemaTask.Filter.DATA_ONLY);

    task.run();
    assertTaskFinishesWithinTimeLimit(task, Duration.ofSeconds(5));
  }

  private void assertTaskFinishesWithinTimeLimit(ImportSchemaTask task, Duration duration) {
    Instant start = Instant.now();
    Instant currentTick = start;
    while (Duration.between(start, currentTick).toMillis() < duration.toMillis()) {
      if (!task.isRunning()) {
        return;
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new AssertException("Unable to wait tick for checking on task");
      }

      currentTick = Instant.now();
    }

    throw new AssertionError("Task did not finish after " + duration.toMillis() + "ms");
  }
}
