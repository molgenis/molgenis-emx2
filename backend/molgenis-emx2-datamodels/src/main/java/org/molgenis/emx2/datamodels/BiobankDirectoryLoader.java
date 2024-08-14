package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class BiobankDirectoryLoader extends ImportDataModelTask {

  public static final String ONTOLOGIES = "DirectoryOntologies";

  private boolean staging = false;

  public BiobankDirectoryLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  public void run() {
    String location = "biobank-directory/";
    if (this.staging) {
      location = "biobank-directory/stagingArea/";
    }

    // create ontology schema
    Database db = getSchema().getDatabase();
    Schema ontologySchema = db.getSchema(ONTOLOGIES);
    if (ontologySchema == null) {
      createSchema(db.createSchema(ONTOLOGIES), "biobank-directory/ontologies/molgenis.csv");
      Schema ontologies = db.getSchema(ONTOLOGIES);
      ontologies.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
    }

    // create biobank-directory or staging schema (which will create tables in ontology schema)
    createSchema(getSchema(), location + "molgenis.csv");

    if (!this.staging) {
      getSchema().addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
    }

    if (ontologySchema == null || !this.staging) {
      // load data into ontology schema
      MolgenisIO.fromClasspathDirectory(
          "biobank-directory/ontologies", db.getSchema(ONTOLOGIES), false);
    }

    // optionally, load demo data
    if (isIncludeDemoData()) {
      MolgenisIO.fromClasspathDirectory(location + "demo", getSchema(), false);
    }
  }

  public ImportDataModelTask setStaging(boolean staging) {
    this.staging = staging;
    return this;
  }
}
