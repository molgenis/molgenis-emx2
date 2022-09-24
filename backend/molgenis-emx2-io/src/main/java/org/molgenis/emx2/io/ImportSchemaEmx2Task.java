package org.molgenis.emx2.io;

import java.util.Objects;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportSchemaEmx2Task extends Task {
  private TableStore store;
  private Schema schema;
  private Filter filter = Filter.ALL;

  public enum Filter {
    METADATA_ONLY,
    DATA_ONLY,
    ALL
  };

  public ImportSchemaEmx2Task(TableStore store, Schema schema, boolean strict) {
    super("Import emx2 format", strict);
    Objects.requireNonNull(store, "tableStore cannot be null");
    Objects.requireNonNull(schema, "schema cannot be null");
    this.store = store;
    this.schema = schema;
  }

  @Override
  public void run() {
    this.start();

    if (!filter.equals(Filter.DATA_ONLY)) {
      Task subTask = new ImportMetadataEmx2Task(schema, store, isStrict());
      this.addSubTask(subTask);
      subTask.run();
    }

    if (!filter.equals(Filter.METADATA_ONLY)) {
      Task dataTask = new ImportDataTask(schema, store, isStrict());
      this.addSubTask(dataTask);
      dataTask.run();
    }
    this.complete();
  }
}
