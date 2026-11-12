package org.molgenis.emx2.io;

import static org.molgenis.emx2.Constants.SETTINGS_TABLE;
import static org.molgenis.emx2.io.emx2.Emx2.MOLGENIS_TABLE;
import static org.molgenis.emx2.io.emx2.Emx2Members.MEMBERS_TABLE;
import static org.molgenis.emx2.io.emx2.Emx2Roles.ROLES_TABLE;

import java.util.Objects;
import org.molgenis.emx2.PermissionEvaluator;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Roles;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

public class ImportMetadataTask extends Task {
  private static final String EMX1_ATTRIBUTES_TABLE = "attributes";
  private final TableStore store;
  private final Schema schema;

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
      if (containsEmx2Metadata()) {
        importTables();
        importRoles();
        importMembers();
        importSettings();
        this.complete();
      } else if (store.containsTable(EMX1_ATTRIBUTES_TABLE)) {
        Emx1.uploadFromStoreToSchema(store, schema);
        this.complete("Imported emx1 metadata");
      } else {
        this.setSkipped("Metadata loading skipped: no metadata included in the file");
      }
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw e;
    }
  }

  private boolean containsEmx2Metadata() {
    return store.containsTable(MOLGENIS_TABLE)
        || store.containsTable(SETTINGS_TABLE)
        || store.containsTable(MEMBERS_TABLE)
        || store.containsTable(ROLES_TABLE);
  }

  private void importTables() {
    if (!store.containsTable(MOLGENIS_TABLE)) {
      addSkipped("Metadata", MOLGENIS_TABLE);
      return;
    }
    schema.migrate(Emx2.fromRowList(store.readTable(MOLGENIS_TABLE)));
    this.addSubTask("Loaded tables and columns from '%s' sheet".formatted(MOLGENIS_TABLE))
        .complete();
  }

  private void importRoles() {
    if (!store.containsTable(ROLES_TABLE)) {
      addSkipped("Roles", ROLES_TABLE);
      return;
    }
    if (!PermissionEvaluator.canManage(schema)) {
      addSkippedBecauseNotManager("Roles", ROLES_TABLE);
      return;
    }
    int count = Emx2Roles.inputRoles(store, schema);
    this.addSubTask("Loaded %s roles from '%s' sheet".formatted(count, ROLES_TABLE)).complete();
  }

  private void importMembers() {
    if (!store.containsTable(MEMBERS_TABLE)) {
      addSkipped("Members", MEMBERS_TABLE);
      return;
    }
    if (!PermissionEvaluator.canManage(schema)) {
      addSkippedBecauseNotManager("Members", MEMBERS_TABLE);
      return;
    }
    int count = Emx2Members.inputMembers(store, schema);
    this.addSubTask("Loaded %s members from '%s' sheet".formatted(count, MEMBERS_TABLE)).complete();
  }

  private void importSettings() {
    if (!store.containsTable(SETTINGS_TABLE)) {
      addSkipped("Settings", SETTINGS_TABLE);
      return;
    }
    Emx2Settings.inputSettings(store, schema);
    this.addSubTask("Loaded settings from '%s' sheet".formatted(SETTINGS_TABLE)).complete();
  }

  private void addSkipped(String what, String sheet) {
    this.addSubTask(
            "%s loading skipped: '%s' sheet not included in the file".formatted(what, sheet))
        .setSkipped();
  }

  private void addSkippedBecauseNotManager(String what, String sheet) {
    this.addSubTask(
            "%s loading skipped: importing '%s' requires Manager permission on schema '%s'"
                .formatted(what, sheet, schema.getName()))
        .setStatus(TaskStatus.WARNING);
  }
}
