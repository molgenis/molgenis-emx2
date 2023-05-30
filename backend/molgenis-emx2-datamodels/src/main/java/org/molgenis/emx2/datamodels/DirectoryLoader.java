package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;

public class DirectoryLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // create catalogue schema (which will create tables in ontology schema)
    createSchema(schema, "directory/molgenis.csv");
    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

    // optionally, load demo data
    if (includeDemoData) {
      // MolgenisIO.fromClasspathDirectory("directory/???", schema, false);
    }
  }
}
