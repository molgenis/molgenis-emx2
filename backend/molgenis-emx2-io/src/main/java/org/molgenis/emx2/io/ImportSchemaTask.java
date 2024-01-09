package org.molgenis.emx2.io;

import java.util.Objects;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportSchemaTask<T extends ImportSchemaTask> extends Task<T> {
  private TableStore tableStore;
  private Schema schema;
  private Filter filter = Filter.ALL;

  public enum Filter {
    METADATA_ONLY,
    DATA_ONLY,
    ALL
  };

  public ImportSchemaTask(String description, TableStore store, Schema schema, boolean strict) {
    super(description, strict);
    Objects.requireNonNull(store, "tableStore cannot be null");
    Objects.requireNonNull(schema, "schema cannot be null");
    this.tableStore = store;
    this.schema = schema;
  }

  public ImportSchemaTask(TableStore store, Schema schema, boolean strict) {
    this("Import from store", store, schema, strict);
  }

  public ImportSchemaTask setFilter(Filter filter) {
    Objects.requireNonNull(filter, "filter cannot be null");
    this.filter = filter;
    return this;
  }

  @Override
  public void run() {
    this.start();
    Task commit = new Task("Committing");
    try {
      schema.tx(
          db -> {
            // import metadata, if any
            Schema s = db.getSchema(schema.getName());

            if (!filter.equals(Filter.DATA_ONLY)) {
              Task metadataTask = new ImportMetadataTask(s, tableStore, isStrict());
              this.addSubTask(metadataTask);
              metadataTask.run();
            }

            if (!filter.equals(Filter.METADATA_ONLY)) {
              Task dataTask = new ImportDataTask(s, tableStore, isStrict());
              this.addSubTask(dataTask);
              dataTask.run();
            }

            // committing
            this.addSubTask(commit.start());
          });
    } catch (Exception e) {
      this.setError("Import failed: " + e.getMessage());
      throw e;
    }
    commit.complete();
    this.complete();
  }
}
