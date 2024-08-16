package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class DirectoryLoader extends ImportDataModelTask {

  public DirectoryLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
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
