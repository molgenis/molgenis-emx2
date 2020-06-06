package org.molgenis.emx2.settings;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class SettingsDataModel {

    public static void create(Schema schema) {
        TableMetadata tm = table("Settings");
        tm.add(column("Schema"));
        tm.add(column("Table"));
        tm.add(column("SettingSection"));
        tm.add(column("SettingName"));
        tm.add(column("SettingType"));
        tm.add(column("SettingDescription"));
        schema.create(tm);
    }

}
