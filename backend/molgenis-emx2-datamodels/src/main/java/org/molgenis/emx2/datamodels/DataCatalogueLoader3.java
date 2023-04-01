package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DataCatalogueLoader3 extends AbstractDataLoader {

  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create ontology schema
    Database db = schema.getDatabase();
    Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
    if (ontologySchema == null) {
      ontologySchema = db.createSchema(CATALOGUE_ONTOLOGIES);
    }

    // create catalogue schema (which will create tables in ontology schema)
    createSchema(schema, "datacatalogue3/molgenis.csv");

    // load data into ontology schema
    MolgenisIO.fromClasspathDirectory("datacatalogue3/CatalogueOntologies", ontologySchema, false);

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("datacatalogue3/Cohorts", schema, false);
    }
  }
}
