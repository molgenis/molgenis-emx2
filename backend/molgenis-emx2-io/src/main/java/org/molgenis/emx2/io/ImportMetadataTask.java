package org.molgenis.emx2.io;

import java.util.Objects;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportMetadataTask extends Task {
  public static final String MOLGENIS = "molgenis";
  private TableStore store;
  private Schema schema;

  public ImportMetadataTask(Schema schema, TableStore store, boolean strict) {
    super("Import metadata", strict);
    Objects.requireNonNull(schema, "schema cannot be null");
    Objects.requireNonNull(store, "tableStore cannot be null");
    this.schema = schema;
    this.store = store;
  }

  @Override
  public void run() {
    this.start();
    try {
      // attempt emx2
      if (store.containsTable(MOLGENIS)
          || store.containsTable("molgenis_settings")
          || store.containsTable("molgenis_members")) {

        if (store.containsTable(MOLGENIS)) {
          schema.migrate(Emx2.fromRowList(store.readTable(MOLGENIS, null)));
          this.addSubTask("Loaded tables and columns from 'molgenis' sheet").complete();
        } else {
          this.addSubTask("Metadata loading skipped: 'molgenis' sheet not included in the file")
              .setSkipped();
        }

        if (store.containsTable("molgenis_members")) {
          int count = Emx2Members.inputRoles(store, schema);
          this.addSubTask("Loaded " + count + " members from 'molgenis_members' sheet").complete();
        } else {
          this.addSubTask(
                  "Members loading skipped: 'molgenis_members' sheet not included in the file")
              .setSkipped();
        }
        if (store.containsTable("molgenis_settings")) {
          Emx2Settings.inputSettings(store, schema);
          this.addSubTask("Loaded settings from 'molgenis_settings' sheet").complete();
        } else {
          this.addSubTask(
                  "Loading settings skipped: 'molgenis_settings' sheet not included in the file")
              .setSkipped();
        }
        this.complete();
      } else
      // attempt emx1
      if (store.containsTable("attributes")) {
        Emx1.uploadFromStoreToSchema(store, schema);
        this.complete("Imported emx1 metadata");
      }
      // otherwise give warning that metadata has been skipped
      else {
        this.setSkipped("Metadata loading skipped: no metadata included in the file");
      }
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw e;
    }
    this.complete();
  }
}
