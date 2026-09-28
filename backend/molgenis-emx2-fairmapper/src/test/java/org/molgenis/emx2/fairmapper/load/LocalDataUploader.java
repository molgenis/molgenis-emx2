package org.molgenis.emx2.fairmapper.load;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
    ImportSchemaTask tasks =
        new ImportSchemaTask(tableStore, schema, false, tables)
            .setFilter(ImportSchemaTask.Filter.DATA_ONLY);

    tasks.run();
    await().atMost(Duration.ofSeconds(5)).until(() -> !tasks.isRunning());
  }
}
