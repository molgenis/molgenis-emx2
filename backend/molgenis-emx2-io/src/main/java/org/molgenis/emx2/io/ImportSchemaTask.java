package org.molgenis.emx2.io;

import static org.molgenis.emx2.tasks.StepStatus.*;

import java.util.Collection;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Step;
import org.molgenis.emx2.tasks.Task;

public class ImportSchemaTask extends Task {
  private TableStore store;
  private Schema schema;

  public ImportSchemaTask(String description, TableStore store, Schema schema) {
    super(description);
    this.store = store;
    this.schema = schema;
  }

  public ImportSchemaTask(TableStore store, Schema schema) {
    super("Import from store");
    this.store = store;
    this.schema = schema;
  }

  public void run() {
    this.start();

    schema.tx(
        s -> {
          try {
            // import metadata, if any
            Task metadataTask = new ImportMetadataTask(schema, store);
            this.add(metadataTask);
            metadataTask.run();

            // create task for the import, including subtasks for each sheet
            boolean skipped = true;
            for (Table table : schema.getTablesSorted()) {
              if (store.containsTable(table.getName())) {
                ImportTableTask importTableTask = new ImportTableTask(store, table);
                this.add(importTableTask);
                importTableTask.run();
                skipped = false;
              }
            }

            // warn for unknown sheet names
            Collection<String> tableNames = schema.getTableNames();
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

            // execute the import tasks
            if (skipped) {
              this.step("Import data skipped: No data sheet included").skipped();
            }
          } catch (MolgenisException e) {
            this.error(e.getMessage());
            this.rollback(this);
            throw e;
          }
        });

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
