package org.molgenis.emx2.io;

import java.util.Objects;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

/**
 * Will import metadata from tableStore, i.e., create the tables (but not load their contents). If
 * no emx2 'molgenis.csv' is available it will attempt emx1 attributes, entities.
 */
public class ImportMetadataTask extends Task {
  public static final String MOLGENIS = "molgenis";
  private TableStore tableStore;
  private Schema schema;

  public ImportMetadataTask(Schema schema, TableStore tableStore, boolean strict) {
    super("Import metadata", strict);
    Objects.requireNonNull(schema, "schema cannot be null");
    Objects.requireNonNull(tableStore, "tableStore cannot be null");
    this.schema = schema;
    this.tableStore = tableStore;
  }

  @Override
  public void run() {
    this.start();
    try {
      // attempt emx2
      if (tableStore.containsTable(MOLGENIS)
          || tableStore.containsTable("molgenis_settings")
          || tableStore.containsTable("molgenis_members")) {

        if (tableStore.containsTable(MOLGENIS)) {
          schema.migrate(Emx2.fromRowList(tableStore.readTable(MOLGENIS)));
          this.addStep("Loaded tables and columns from 'molgenis' sheet").complete();
        } else {
          this.addStep("Metadata loading skipped: 'molgenis' sheet not included in the file")
              .skipped();
        }

        if (tableStore.containsTable("molgenis_members")) {
          int count = Emx2Members.inputRoles(tableStore, schema);
          this.addStep("Loaded " + count + " members from 'molgenis_members' sheet").complete();
        } else {
          this.addStep("Members loading skipped: 'molgenis_members' sheet not included in the file")
              .skipped();
        }
        if (tableStore.containsTable("molgenis_settings")) {
          Emx2Settings.inputSettings(tableStore, schema);
          this.addStep("Loaded settings from 'molgenis_settings' sheet").complete();
        } else {
          this.addStep(
                  "Loading settings skipped: 'molgenis_settings' sheet not included in the file")
              .skipped();
        }
        this.complete();
      } else
      // attempt emx1
      if (tableStore.containsTable("attributes")) {
        Emx1.uploadFromStoreToSchema(tableStore, schema);
        this.complete("Imported emx1 metadata");
      }
      // otherwise give warning that metadata has been skipped
      else {
        this.skipped("Metadata loading skipped: no metadata included in the file");
      }
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw e;
    }
    this.complete();
  }
}
