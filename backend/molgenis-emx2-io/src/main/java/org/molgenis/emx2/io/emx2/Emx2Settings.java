package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2Settings {
  public static final String SETTINGS_TABLE = "molgenis_settings";
  public static final String SETTINGS_KEY = "key";
  public static final String SETTINGS_VALUE = "value";

  public static void outputSettings(TableStore store, Schema schema) {
    List<Row> settings = new ArrayList<>();
    for (Map.Entry<String, String> setting : schema.getMetadata().getSettings().entrySet()) {
      settings.add(row(SETTINGS_KEY, setting.getKey(), SETTINGS_VALUE, setting.getValue()));
    }
    if (settings.size() > 0) {
      store.writeTable(SETTINGS_TABLE, settings);
    }
  }

  public static void inputSettings(TableStore store, Schema schema) {
    Map<String, String> settings = new LinkedHashMap<>();
    for (Row setting : store.readTable(SETTINGS_TABLE)) {
      settings.put(setting.getString(SETTINGS_KEY), setting.getString(SETTINGS_VALUE));
    }
    schema.getMetadata().setSettings(settings);
  }
}
