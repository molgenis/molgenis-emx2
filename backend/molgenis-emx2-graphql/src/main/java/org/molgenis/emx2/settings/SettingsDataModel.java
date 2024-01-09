package org.molgenis.emx2.settings;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

public class SettingsDataModel {

  private SettingsDataModel() {
    // hide constructor
  }

  public static void create(Schema schema) {
    TableMetadata tm = table("Settings");
    tm.add(column("schema").setKey(1));
    tm.add(column("key").setKey(1));
    tm.add(column("value"));
    schema.create(tm);
  }
}
