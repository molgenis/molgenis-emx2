package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class ErnTestLoader extends ImportDataModelTask {

  public ErnTestLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    try {
      createSchema(getSchema(), "ern_test/molgenis.csv");
      getSchema().addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
      MolgenisIO.fromClasspathDirectory("ern_test/molgenis", getSchema(), false);
      MolgenisIO.fromClasspathDirectory("ern_test/ontologies", getSchema(), false);
      this.complete();
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw new MolgenisException("Failed to create schema", e);
    }
  }
}
