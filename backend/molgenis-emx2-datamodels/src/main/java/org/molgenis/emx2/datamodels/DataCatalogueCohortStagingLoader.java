package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueLoader.*;
import static org.molgenis.emx2.datamodels.DataCatalogueNetworkStagingLoader.SHARED_SCHEMA;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DataCatalogueCohortStagingLoader implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create ontology schema
    Database db = schema.getDatabase();
    Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
    if (ontologySchema == null) {
      ontologySchema = db.createSchema(CATALOGUE_ONTOLOGIES);
    }

    Schema sharedSchema = db.getSchema(SHARED_SCHEMA);
    if (sharedSchema == null) {
      sharedSchema = db.createSchema(SHARED_SCHEMA);
      // create the shared schema
      createSchema(sharedSchema, "datacatalogue/stagingShared/molgenis.csv");
    }

    // create the schema
    createSchema(schema, "datacatalogue/stagingCohorts/molgenis.csv");

    // load data into ontology schema
    MolgenisIO.fromClasspathDirectory("datacatalogue/CatalogueOntologies", ontologySchema, false);
  }
}
