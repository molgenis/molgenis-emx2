package org.molgenis.emx2.io;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.processor.ImportRowProcessor;
import org.molgenis.emx2.io.tablestore.processor.ValidatePkeyProcessor;
import org.molgenis.emx2.tasks.Task;

public class ImportTableTask extends Task {
  private Table table;
  private TableStore source;

  public ImportTableTask(TableStore source, Table table, boolean strict) {
    super("Import table " + table.getName(), strict);
    Objects.requireNonNull(source, "tableStore cannot be null");
    Objects.requireNonNull(table, "table cannot be null");
    this.table = table;
    this.source = source;
  }

  @Override
  public void run() {
    this.start();

    // validate uniqueness of the keys in the set
    this.setDescription(
        "Table " + table.getName() + ": Counting rows & checking that all key columns are unique");
    source.processTable(table.getName(), new ValidatePkeyProcessor(table.getMetadata(), this));

    // execute the actual loading, we can use index to find the size
    this.setTotal(this.getProgress());
    this.setDescription("Importing rows into " + table.getName());

    try {
      source.processTable(table.getName(), new ImportRowProcessor(table, this));
    } catch (Exception e) {
      this.setError("Import table (%s) failed: %s".formatted(table.getName(), e.getMessage()));
      throw e;
    }

    // done
    if (getProgress() > 0) {
      this.complete(String.format("Modified %s rows in %s", getProgress(), table.getName()));
    } else {
      this.setSkipped(String.format("Skipped table %s : sheet was empty", table.getName()));
    }
  }
}
