package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2Settings {

  private Emx2Settings() {
    // prevent
  }

  public static void outputSettings(TableStore store, Schema schema) {
    List<Row> settings = new ArrayList<>();

    // schema settings
    for (Setting setting : schema.getMetadata().getSettings()) {
      settings.add(
          row(
              SETTINGS_NAME,
              setting.key(),
              SETTINGS_VALUE,
              setting.value(),
              SETTINGS_USER,
              setting.user()));
    }

    // table settings
    for (TableMetadata table : schema.getMetadata().getTables()) {
      for (Setting setting : table.getSettings()) {
        settings.add(
            row(
                TABLE,
                table.getTableName(),
                SETTINGS_NAME,
                setting.key(),
                SETTINGS_VALUE,
                setting.value()));
      }
    }

    if (!settings.isEmpty()) {
      store.writeTable(
          Constants.SETTINGS_TABLE, List.of(TABLE, SETTINGS_NAME, SETTINGS_VALUE), settings);
    }
  }

  public static void inputSettings(TableStore store, Schema schema) {
    int row = 1;
    if (store.containsTable(Constants.SETTINGS_TABLE)) {
      for (Row setting : store.readTable(Constants.SETTINGS_TABLE)) {
        String tableName = setting.getString(TABLE);
        if (tableName != null) {
          Table table = schema.getTable(tableName);
          if (table == null) {
            throw new MolgenisException(
                "Loading of setting failed on line " + row + ": table '" + tableName);
          }
          table
              .getMetadata()
              .setSetting(setting.getString(SETTINGS_NAME), setting.getString(SETTINGS_VALUE));
        } else {
          schema
              .getMetadata()
              .setSetting(setting.getString(SETTINGS_NAME), setting.getString(SETTINGS_VALUE));
        }
      }
    }
  }
}
