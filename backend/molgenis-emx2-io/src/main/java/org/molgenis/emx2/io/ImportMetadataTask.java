package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportMetadataTask extends Task {
  private TableStore store;
  private Schema schema;

  public ImportMetadataTask(Schema schema, TableStore store, boolean strict) {
    super("Import metadata", strict);
    this.schema = schema;
    this.store = store;
  }

  public void run() {
    this.start();
    try {
      if (store.containsTable("attributes")) {
        Emx1.uploadFromStoreToSchema(store, schema);
        this.complete("Imported emx1 metadata");
      }

      if (store.containsTable("molgenis")
          || store.containsTable("molgenis_settings")
          || store.containsTable("molgenis_members")) {

        if (store.containsTable("molgenis")) {
          schema.migrate(Emx2.fromRowList(store.readTable("molgenis")));
          this.step("Loaded tables and columns from 'molgenis' sheet").complete();
        } else {
          this.step("Metadata loading skipped: 'molgenis' sheet not included in the file")
              .skipped();
        }

        if (store.containsTable("molgenis_members")) {
          int count = Emx2Members.inputRoles(store, schema);
          this.step("Loaded " + count + " members from 'molgenis_members' sheet").complete();
        } else {
          this.step("Members loading skipped: 'molgenis_members' sheet not included in the file")
              .skipped();
        }
        if (store.containsTable("molgenis_settings")) {
          Emx2Settings.inputSettings(store, schema);
          this.step("Loaded settings from 'molgenis_settings' sheet").complete();
        } else {
          this.step("Loading settings skipped: 'molgenis_settings' sheet not included in the file")
              .skipped();
        }
        this.complete();
      } else {
        this.skipped("Metadata loading skipped: no metadata included in the file");
      }
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw e;
    }
  }
}
