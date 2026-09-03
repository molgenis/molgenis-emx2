package org.molgenis.emx2.fairmapper.tasks;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseDataLoader implements DataLoader {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseDataLoader.class);

  private final Schema schema;
  private final String[] tables;

  public DatabaseDataLoader(Schema schema, String... tables) {
    this.schema = schema;
    this.tables = tables;
  }

  @Override
  public void load(InMemoryTableStore tableStore) {
    ImportSchemaTask tasks =
        new ImportSchemaTask(tableStore, schema, false, tables)
            .setFilter(ImportSchemaTask.Filter.DATA_ONLY);

    tasks.run();
    while (tasks.isRunning()) {
      logger.info("waiting...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new MolgenisException("Something went wrong when uploading the data: ", e);
      }
    }
  }
}
