package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class ProjectManagerLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    createSchema(schema, "projectmanager/molgenis.csv");
    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("projectmanager/data", schema, false);
    }

    // get the menu
    schema
        .getMetadata()
        .setSetting(
            "menu",
            "[{\"label\":\"Project planning\",\"href\":\"projectmanager\",\"key\":\"7u3aa\",\"submenu\":[],\"role\":\"Viewer\"},{\"label\":\"Person planning\",\"href\":\"projectmanager/#/persons\",\"key\":\"t5gm7c\",\"submenu\":[],\"role\":\"Viewer\"},{\"label\":\"Tables\",\"href\":\"tables\",\"role\":\"Viewer\",\"key\":\"v08qu\",\"submenu\":[]},{\"label\":\"Schema\",\"href\":\"schema\",\"role\":\"Manager\",\"key\":\"9hcey\",\"submenu\":[]},{\"label\":\"Up/Download\",\"href\":\"updownload\",\"role\":\"Editor\",\"key\":\"mbt16g\",\"submenu\":[]},{\"label\":\"Graphql\",\"href\":\"graphql-playground\",\"role\":\"Viewer\",\"key\":\"kyjbbb\",\"submenu\":[]},{\"label\":\"Settings\",\"href\":\"settings\",\"role\":\"Manager\",\"key\":\"ev7sm\",\"submenu\":[]},{\"label\":\"Help\",\"href\":\"docs\",\"role\":\"Viewer\",\"key\":\"s8ug2c\",\"submenu\":[]}]");
  }
}
