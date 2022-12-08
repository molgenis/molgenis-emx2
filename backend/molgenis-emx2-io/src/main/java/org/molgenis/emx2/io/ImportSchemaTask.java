package org.molgenis.emx2.io;

import java.util.Objects;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportSchemaTask extends Task {
  private TableStore store;
  private Schema schema;

  public enum Filter {
    METADATA_ONLY,
    DATA_ONLY,
    ALL
  };

  public ImportSchemaTask(String description, TableStore store, Schema schema, boolean strict) {
    super(description, strict);
    Objects.requireNonNull(store, "tableStore cannot be null");
    Objects.requireNonNull(schema, "schema cannot be null");
    this.store = store;
    this.schema = schema;
  }

  public ImportSchemaTask(TableStore store, Schema schema, boolean strict) {
    this("Import from store", store, schema, strict);
  }

  @Override
  public void run() {
    this.start();
    Task commit = new Task("Committing");
    try {
      schema.tx(
          db -> {
            // import metadata, if any
            Schema schema = db.getSchema(this.schema.getName());

            // attempt emx1
            if (store.containsTable("attributes")) {
              Task subTask = new ImportSchemaEmx1Task(store, schema);
              this.addSubTask(subTask);
              subTask.run();
            } else {
              Task subTask = new ImportSchemaEmx2Task(store, schema, isStrict());
              this.addSubTask(subTask);
              subTask.run();
            }

            // committing will start now
            this.addSubTask(commit);
            commit.start();
          });
    } catch (Exception e) {
      this.setError("Import failed: " + e.getMessage());
      throw e;
    }
    commit.complete();
    this.complete();
  }
}
