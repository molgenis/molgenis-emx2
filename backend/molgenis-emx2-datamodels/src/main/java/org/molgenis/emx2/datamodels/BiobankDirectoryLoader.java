package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class BiobankDirectoryLoader extends AbstractDataLoader {

  public static final String ONTOLOGIES = "DirectoryOntologies";

  private final boolean staging;

  public BiobankDirectoryLoader(boolean staging) {
    this.staging = staging;
  }

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    String location = "biobank-directory/";
    if (this.staging) {
      location = "biobank-directory/stagingArea/";
    }

    // create ontology schema
    Database db = schema.getDatabase();
    Schema ontologySchema = db.getSchema(ONTOLOGIES);
    if (ontologySchema == null) {
      createSchema(db.createSchema(ONTOLOGIES), "biobank-directory/ontologies/molgenis.csv");
      Schema ontologies = db.getSchema(ONTOLOGIES);
      ontologies.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
    }

    // create biobank-directory or staging schema (which will create tables in ontology schema)
    createSchema(schema, location + "molgenis.csv");
    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

    if (ontologySchema == null || !this.staging) {
      // load data into ontology schema
      MolgenisIO.fromClasspathDirectory(
          "biobank-directory/ontologies", db.getSchema(ONTOLOGIES), false);
    }

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory(location + "demo", schema, false);
    }
  }
}
