package org.molgenis.emx2.io;

import static org.molgenis.emx2.tasks.StepStatus.*;

import java.util.Collection;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Step;
import org.molgenis.emx2.tasks.Task;

public class ImportSchemaTask extends Task {
  private TableStore store;
  private Schema schema;

  public ImportSchemaTask(String description, TableStore store, Schema schema, boolean strict) {
    super(description, strict);
    this.store = store;
    this.schema = schema;
  }

  public ImportSchemaTask(TableStore store, Schema schema, boolean strict) {
    super("Import from store", strict);
    this.store = store;
    this.schema = schema;
  }

  public void run() {
    this.start();
    try {
      schema.tx(
          db -> {
            // import metadata, if any
            Schema s = db.getSchema(schema.getName());
            Task metadataTask = new ImportMetadataTask(s, store, isStrict());
            this.add(metadataTask);
            metadataTask.run();

            // in case of emx1
            boolean skipped = true;
            if (store.containsTable("attributes")) {

            } else {

              // create task for the import, including subtasks for each sheet
              for (Table table : s.getTablesSorted()) {
                if (store.containsTable(table.getName())) {
                  ImportTableTask importTableTask = new ImportTableTask(store, table, isStrict());
                  this.add(importTableTask);
                  importTableTask.run();
                  skipped = false;
                }
              }

              // warn for unknown sheet names
              Collection<String> tableNames = s.getTableNames();
              for (String sheet : store.tableNames()) {
                if (!"molgenis".equals(sheet)
                    && !"molgenis_settings".equals(sheet)
                    && !"molgenis_members".equals(sheet)
                    && !tableNames.contains(sheet)) {
                  this.step(
                          "Sheet with name '"
                              + sheet
                              + "' was skipped: no table with that name found")
                      .skipped();
                }
              }
            }

            // execute the import tasks
            if (skipped) {
              this.step("Import data skipped: No data sheet included").skipped();
            }

            // commit
            this.step("Committing data (may take a while)").start();
          });
      this.getSteps().get(this.getSteps().size() - 1).setDescription("Committed data").complete();
    } catch (Exception e) {
      this.getSteps().get(this.getSteps().size() - 1).error("Commit failed: " + e.getMessage());
      this.rollback(this);
      this.error("Import failed: " + e.getMessage());
      throw e;
    }
    this.complete();
  }

  private void rollback(Task task) {
    for (Step step : task.getSteps()) {
      if (step.getStatus().equals(COMPLETED)) {
        step.setStatus(SKIPPED);
        step.setDescription("Rolled back: " + step.getDescription());
      }
      if (step instanceof Task) {
        this.rollback((Task) step);
      }
    }
  }
}
