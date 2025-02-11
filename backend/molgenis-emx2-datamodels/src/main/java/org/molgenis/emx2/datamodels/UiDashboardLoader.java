package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class UiDashboardLoader extends ImportDataModelTask {

  public UiDashboardLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    try {
      createSchema(getSchema(), "ui_dashboards/molgenis.csv");
      getSchema().addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
      MolgenisIO.fromClasspathDirectory("ui_dashboards/ontologies", getSchema(), false);

      if (isIncludeDemoData()) {
        MolgenisIO.fromClasspathDirectory("ui_dashboards/demodata", getSchema(), false);
      }
      this.complete();
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw new MolgenisException("Failed to create schema", e);
    }
  }
}
