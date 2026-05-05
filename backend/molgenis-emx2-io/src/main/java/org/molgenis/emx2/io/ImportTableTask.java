package org.molgenis.emx2.io;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.processor.ImportRowProcessor;
import org.molgenis.emx2.io.tablestore.processor.ValidatePkeyProcessor;
import org.molgenis.emx2.tasks.Task;

public class ImportTableTask extends Task {
  private final Table table;
  private final TableStore source;

  public ImportTableTask(TableStore source, Table table, boolean strict) {
    super("Import table %s".formatted(table.getName()), strict);
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
        "Table %s: Counting rows & checking that all key columns are unique"
            .formatted(table.getName()));
    source.processTable(table.getName(), new ValidatePkeyProcessor(table.getMetadata(), this));

    // execute the actual loading, we can use index to find the size
    this.setTotal(this.getProgress());
    this.setDescription("Importing rows into %s".formatted(table.getName()));

    try {
      source.processTable(table.getName(), new ImportRowProcessor(table, this));
    } catch (Exception e) {
      this.setError("Import table (%s) failed: %s".formatted(table.getName(), e.getMessage()));
      throw e;
    }

    // done
    if (getProgress() > 0) {
      this.complete("Modified %s rows in %s".formatted(getProgress(), table.getName()));
    } else {
      this.setSkipped("Skipped table %s : sheet was empty".formatted(table.getName()));
    }
  }
}
