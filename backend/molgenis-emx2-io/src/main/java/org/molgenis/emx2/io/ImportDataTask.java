package org.molgenis.emx2.io;

import static org.molgenis.emx2.Constants.SETTINGS_TABLE;
import static org.molgenis.emx2.io.emx2.Emx2.MOLGENIS_TABLE;
import static org.molgenis.emx2.io.emx2.Emx2Members.MEMBERS_TABLE;
import static org.molgenis.emx2.io.emx2.Emx2Roles.ROLES_TABLE;

import java.util.*;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

/**
 * Task to import schema from a table store, i.e., will run ImportMetadataTask and for each table a
 * ImportTableTask.
 */
public class ImportDataTask extends Task {
  private final TableStore tableStore;
  private final Schema schema;
  private Set<String> includeTableNames;

  public ImportDataTask(
      String description,
      TableStore store,
      Schema schema,
      boolean strict,
      String... includeTableNames) {
    super(description, strict);
    Objects.requireNonNull(store, "tableStore cannot be null");
    Objects.requireNonNull(schema, "schema cannot be null");
    this.tableStore = store;
    this.schema = schema;

    if (includeTableNames.length > 0) {
      this.includeTableNames = new HashSet<>(Arrays.asList(includeTableNames));
    }
  }

  public ImportDataTask(
      Schema schema, TableStore store, boolean strict, String... includeTableNames) {
    this("Import from store", store, schema, strict, includeTableNames);
  }

  @Override
  public void run() {
    this.start();

    // create a task for each table
    boolean skipped = true;

    // create task for the import, including subtasks for each sheet
    for (Table table : schema.getTablesSorted()) {
      if (tableStore.containsTable(table.getName())
          && (includeTableNames == null || includeTableNames.contains(table.getName()))) {
        ImportTableTask importTableTask = new ImportTableTask(tableStore, table, isStrict());
        this.addSubTask(importTableTask);
        importTableTask.run();
        skipped = false;
      }
    }

    // check what files we skipped
    try {
      for (String tableName : tableStore.getTableNames()) {
        String sheet = tableName.toLowerCase().replace(" ", "");
        if (!sheet.startsWith("_files/")
            && !MOLGENIS_TABLE.equals(sheet)
            && !SETTINGS_TABLE.equals(sheet)
            && !MEMBERS_TABLE.equals(sheet)
            && !ROLES_TABLE.equals(sheet)
            && !schema.hasTableWithNameOrIdCaseInsensitive(sheet)) {
          this.addSubTask(
                  "Skipped sheet with name '%s': no table with that name found"
                      .formatted(tableName))
              .setSkipped();
        }
      }
    } catch (UnsupportedOperationException e) {
      // ignore, not important
    }

    // execute the import tasks
    if (skipped) {
      this.addSubTask("Import data skipped: No data sheet included").setSkipped();
    }
    this.complete();
  }
}
