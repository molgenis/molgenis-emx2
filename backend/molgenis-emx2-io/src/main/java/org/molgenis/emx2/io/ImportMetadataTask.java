package org.molgenis.emx2.io;

import static org.molgenis.emx2.io.emx2.Emx2.inputMetadata;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportMetadataTask extends Task {
  private TableStore store;
  private Schema schema;

  public ImportMetadataTask(Schema schema, TableStore store) {
    super("Import metadata");
    this.schema = schema;
    this.store = store;
  }

  public void run() {
    this.start();
    try {
      if (store.containsTable("attributes")) {
        Emx1.uploadFromStoreToSchema(store, schema);
        this.complete("Imported emx1 metadata");
      } else if (store.containsTable("molgenis")) {
        inputMetadata(store, schema);
        this.complete("Imported emx2 metadata");
      } else {
        this.skipped("Metadata loading skipped: not included in the file");
      }
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw e;
    }
  }
}
