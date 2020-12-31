package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2Settings {

  public static void outputSettings(TableStore store, Schema schema) {
    List<Row> settings = new ArrayList<>();
    for (Setting setting : schema.getMetadata().getSettings()) {
      settings.add(
          row(
              Constants.SETTINGS_NAME,
              setting.getKey(),
              Constants.SETTINGS_VALUE,
              setting.getValue()));
    }
    if (settings.size() > 0) {
      store.writeTable(Constants.SETTINGS_TABLE, settings);
    }
  }

  public static void inputSettings(TableStore store, Schema schema) {
    if (store.containsTable(Constants.SETTINGS_TABLE)) {
      for (Row setting : store.readTable(Constants.SETTINGS_TABLE)) {
        schema
            .getMetadata()
            .setSetting(
                setting.getString(Constants.SETTINGS_NAME),
                setting.getString(Constants.SETTINGS_VALUE));
      }
    }
  }
}
