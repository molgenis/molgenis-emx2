package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class DirectoryLoader extends ImportDataModelTask {

  public DirectoryLoader(Database database, String schemaName, Boolean includeDemoData) {
    super(database, schemaName, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    try {
      // create catalogue schema (which will create tables in ontology schema)
      createSchema("directory/molgenis.csv");
      getSchema().addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

      // optionally, load demo data
      if (isIncludeDemoData()) {
        MolgenisIO.fromClasspathDirectory("directory/data", getSchema(), false);
      }
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw (e);
    }
    this.complete();
  }
}
