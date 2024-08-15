package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class DashboardLoader extends ImportDataModelTask {

  public DashboardLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    createSchema(getSchema(), "dashboard/molgenis.csv");
    getSchema().addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

    if (isIncludeDemoData()) {
      MolgenisIO.fromClasspathDirectory("dashboard/demodata/ontologies", getSchema(), false);
      MolgenisIO.fromClasspathDirectory("dashboard/demodata/datasets", getSchema(), false);
    }

    getSchema()
        .getMetadata()
        .setSetting(
            "menu",
            "[{'label': 'Demo', 'href': './molgenis-viz/', 'key': 'mwlu8b', 'submenu': [], 'role': 'Viewer'}, {'label': 'Tables', 'href': 'tables', 'role': 'Viewer', 'key': '3ywoaq', 'submenu': []}, {'label': 'Schema', 'href': 'schema', 'role': 'Manager', 'key': 'd0y34a', 'submenu': []}, {'label': 'Up/Download', 'href': 'updownload', 'role': 'Editor', 'key': 'r2mc15', 'submenu': []}, {'label': 'Graphql', 'href': 'graphql-playground', 'role': 'Viewer', 'key': 're5u4i', 'submenu': []}, {'label': 'Settings', 'href': 'settings', 'role': 'Manager', 'key': 'v1zouk', 'submenu': []}, {'label': 'Help', 'href': 'docs', 'role': 'Viewer', 'key': 'dikoff', 'submenu': []}]");
    this.complete();
  }
}
