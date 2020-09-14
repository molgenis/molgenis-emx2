package org.molgenis.emx2.settings;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class SettingsDataModel {

  public static void create(Schema schema) {
    TableMetadata tm = table("Settings");
    tm.add(column("schema").key(1));
    tm.add(column("key").key(1));
    tm.add(column("value"));
    schema.create(tm);
  }
}
